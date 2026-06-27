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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-13T08:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthSessionRepository sessionRepository;

    @Mock
    private OwnershipMigrationService ownershipMigrationService;

    private PasswordHasher passwordHasher;
    private AuthTokenService tokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasher();
        tokenService = new AuthTokenService();
        authService = new AuthService(
            userRepository,
            sessionRepository,
            ownershipMigrationService,
            passwordHasher,
            tokenService,
            CLOCK
        );
    }

    @Test
    @DisplayName("register creates first user, claims unowned data, and returns bearer token")
    void registerCreatesFirstUserClaimsUnownedDataAndReturnsBearerToken() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(
            new AuthRegisterRequest(" Alice ", "secret123", "Alice Zhang")
        );

        assertThat(response.token()).isNotBlank();
        assertThat(response.user()).isEqualTo(new CurrentUserDTO(1L, "alice", "Alice Zhang", false));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice");
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        verify(ownershipMigrationService).claimUnownedData(userCaptor.getValue());
    }

    @Test
    @DisplayName("login rejects a wrong password and does not create a session")
    void loginRejectsWrongPasswordAndDoesNotCreateSession() {
        PasswordHasher.PasswordHash stored = passwordHasher.hash("right-password");
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setUsername("alice");
        user.setDisplayName("Alice");
        user.setPasswordSalt(stored.salt());
        user.setPasswordHash(stored.hash());

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new AuthLoginRequest("alice", "wrong-password")))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());

        verify(sessionRepository, never()).save(any(AuthSessionEntity.class));
    }

    @Test
    @DisplayName("authenticate returns empty for expired or revoked token")
    void authenticateReturnsEmptyForExpiredOrRevokedToken() {
        AuthSessionEntity expired = new AuthSessionEntity();
        expired.setTokenHash(tokenService.hashToken("expired-token"));
        expired.setExpiresAt(Instant.parse("2026-04-13T07:59:59Z"));

        AuthSessionEntity revoked = new AuthSessionEntity();
        revoked.setTokenHash(tokenService.hashToken("revoked-token"));
        revoked.setExpiresAt(Instant.parse("2026-04-14T08:00:00Z"));
        revoked.setRevokedAt(Instant.parse("2026-04-13T08:00:00Z"));

        when(sessionRepository.findByTokenHash(expired.getTokenHash())).thenReturn(Optional.of(expired));
        when(sessionRepository.findByTokenHash(revoked.getTokenHash())).thenReturn(Optional.of(revoked));

        assertThat(authService.authenticate("expired-token")).isEmpty();
        assertThat(authService.authenticate("revoked-token")).isEmpty();
    }
}
