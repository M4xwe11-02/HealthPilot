package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.config.EmailAuthProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SmtpVerificationMailSender implements VerificationMailSender {

    private final JavaMailSender mailSender;
    private final EmailAuthProperties properties;

    @Override
    public void sendCode(String email, String code, Duration validity) {
        if (!StringUtils.hasText(properties.getFrom())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮箱服务尚未配置");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.getFrom().trim(), properties.getSenderName());
            helper.setTo(email);
            helper.setSubject("Health Guard 登录验证码");
            helper.setText(buildMessage(code, validity), false);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "验证码邮件发送失败，请稍后重试");
        }
    }

    private String buildMessage(String code, Duration validity) {
        long minutes = Math.max(1, validity.toMinutes());
        return "您的 Health Guard 登录验证码是：" + code + "\n\n"
            + "验证码将在 " + minutes + " 分钟后失效，请勿转发给他人。\n"
            + "如果不是您本人操作，请忽略此邮件。";
    }
}
