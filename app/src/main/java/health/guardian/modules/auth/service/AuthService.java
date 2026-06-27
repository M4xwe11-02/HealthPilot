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

        boolean firstUser = userRepository.count() == 0;
        PasswordHasher.PasswordHash passwordHash = passwordHasher.hash(password);

        UserEntity user = new UserEntity();
        user.setUsername(username);
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
}
