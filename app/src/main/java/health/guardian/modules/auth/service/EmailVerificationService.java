package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.config.EmailAuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final EmailAuthProperties properties;
    private final EmailCodeStore codeStore;
    private final VerificationMailSender mailSender;
    private final PasswordHasher passwordHasher;
    private final AuthTokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void sendCode(String rawEmail) {
        ensureEnabledAndConfigured();
        String email = normalizeEmail(rawEmail);
        String identity = identity(email);

        EmailCodeStore.SendReservation reservation = codeStore.reserveSend(
            identity,
            properties.getResendCooldown(),
            properties.getSendLimitWindow(),
            properties.getMaxSendsPerWindow()
        );
        if (reservation == EmailCodeStore.SendReservation.COOLDOWN) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
        }
        if (reservation == EmailCodeStore.SendReservation.LIMIT_REACHED) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "验证码发送次数已达上限，请稍后再试");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        try {
            codeStore.saveCode(identity, passwordHasher.hash(code), properties.getCodeTtl());
            mailSender.sendCode(email, code, properties.getCodeTtl());
        } catch (RuntimeException e) {
            codeStore.removeCode(identity);
            codeStore.releaseSend(identity);
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        }
    }

    public String verifyAndConsume(String rawEmail, String code) {
        ensureEnabled();
        String email = normalizeEmail(rawEmail);
        if (code == null || !code.matches("\\d{6}")) {
            throw invalidCodeException();
        }

        EmailCodeStore.ConsumeResult result = codeStore.consume(
            identity(email),
            code,
            properties.getMaxVerifyAttempts()
        );
        if (result != EmailCodeStore.ConsumeResult.SUCCESS) {
            throw invalidCodeException();
        }
        return email;
    }

    private void ensureEnabledAndConfigured() {
        ensureEnabled();
        if (!StringUtils.hasText(properties.getFrom())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮箱服务尚未配置");
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱验证码登录未启用");
        }
    }

    private String normalizeEmail(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
        }
        return email;
    }

    private String identity(String email) {
        return tokenService.hashToken(email);
    }

    private BusinessException invalidCodeException() {
        return new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误或已过期");
    }
}
