package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.AuthLoginRequest;
import health.guardian.modules.auth.model.AuthRegisterRequest;
import health.guardian.modules.auth.model.AuthResponse;
import health.guardian.modules.auth.model.AuthSessionEntity;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.auth.repository.AuthSessionRepository;
import health.guardian.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration SESSION_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final OwnershipMigrationService ownershipMigrationService;
    private final EmailAccountMergeService emailAccountMergeService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordHasher passwordHasher;
    private final AuthTokenService tokenService;
    private final Clock clock;

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());
        String displayName = normalizeDisplayName(request.displayName(), username);

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名已存在");
        }

        String email = verifyRegistrationEmail(request.email(), request.code());
        retireConflictingEmailOwner(null, email);

        boolean firstUser = userRepository.count() == 0;
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(password);

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordSalt(passwordHash.salt());
        user.setPasswordHash(passwordHash.hash());

        UserEntity saved = userRepository.save(user);
        if (firstUser) {
            ownershipMigrationService.claimUnownedData(saved);
        }

        return issueSession(saved);
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        String username = normalizeUsername(request.username());
        String password = normalizePassword(request.password());

        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误"));

        if (!passwordHasher.verify(password, user.getPasswordSalt(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        return issueSession(user);
    }

    @Transactional
    public AuthResponse loginWithEmail(String email) {
        UserEntity user = userRepository.findByEmail(email)
            .orElseGet(() -> createEmailUser(email));
        return issueSession(user);
    }

    @Transactional
    public CurrentUserDTO bindEmail(Long userId, String email) {
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "登录状态已失效，请重新登录"));

        retireConflictingEmailOwner(currentUser.getId(), email);

        currentUser.setEmail(email);
        return CurrentUserDTO.from(userRepository.save(currentUser));
    }

    @Transactional(readOnly = true)
    public Optional<CurrentUserDTO> authenticate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = tokenService.hashToken(token.trim());
        return sessionRepository.findByTokenHash(tokenHash)
            .filter(session -> session.isActive(clock.instant()))
            .map(AuthSessionEntity::getUser)
            .map(CurrentUserDTO::from);
    }

    @Transactional
    public void logout(String authorizationHeader) {
        tokenService.extractBearerToken(authorizationHeader)
            .map(tokenService::hashToken)
            .flatMap(sessionRepository::findByTokenHash)
            .ifPresent(session -> {
                session.setRevokedAt(clock.instant());
                sessionRepository.save(session);
            });
    }

    private AuthResponse issueSession(UserEntity user) {
        Instant now = clock.instant();
        String token = tokenService.issueToken();

        AuthSessionEntity session = new AuthSessionEntity();
        session.setUser(user);
        session.setTokenHash(tokenService.hashToken(token));
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(SESSION_TTL));
        sessionRepository.save(session);

        return new AuthResponse(token, CurrentUserDTO.from(user));
    }

    private UserEntity createEmailUser(String email) {
        boolean firstUser = userRepository.count() == 0;
        String identityHash = tokenService.hashToken(email);
        String username = "mail_" + identityHash.substring(0, 24);
        if (userRepository.existsByUsername(username)) {
            username = "mail_" + identityHash.substring(0, 40);
        }

        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(tokenService.issueToken());
        String localPart = email.substring(0, email.indexOf('@'));

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(localPart.substring(0, Math.min(localPart.length(), 100)));
        user.setPasswordSalt(passwordHash.salt());
        user.setPasswordHash(passwordHash.hash());

        UserEntity saved = userRepository.save(user);
        if (firstUser) {
            ownershipMigrationService.claimUnownedData(saved);
        }
        return saved;
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        if (normalized.length() > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能超过50个字符");
        }
        return normalized;
    }

    private String normalizePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度不能少于4个字符");
        }
        return password;
    }

    private String normalizeDisplayName(String displayName, String username) {
        if (displayName == null || displayName.trim().isBlank()) {
            return username;
        }
        return displayName.trim();
    }

    private String verifyRegistrationEmail(String email, String code) {
        boolean hasEmail = StringUtils.hasText(email);
        boolean hasCode = StringUtils.hasText(code);
        if (!hasEmail && !hasCode) {
            return null;
        }
        if (!hasEmail) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入邮箱后再填写验证码");
        }
        if (!hasCode) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "填写邮箱后必须输入验证码");
        }
        return emailVerificationService.verifyAndConsume(email, code);
    }

    private void retireConflictingEmailOwner(Long currentUserId, String email) {
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email)
            .filter(owner -> currentUserId == null || !owner.getId().equals(currentUserId))
            .ifPresent(owner -> emailAccountMergeService.retireEmptyGeneratedAccount(owner, email));
    }
}
