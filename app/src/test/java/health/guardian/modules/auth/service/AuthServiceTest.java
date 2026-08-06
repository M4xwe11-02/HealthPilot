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

    @Mock
    private EmailAccountMergeService emailAccountMergeService;

    @Mock
    private EmailVerificationService emailVerificationService;

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
            emailAccountMergeService,
            emailVerificationService,
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
            new AuthRegisterRequest(" Alice ", "secret123", "Alice Zhang", null, null)
        );

        assertThat(response.token()).isNotBlank();
        assertThat(response.user()).isEqualTo(new CurrentUserDTO(1L, "alice", "Alice Zhang", false, null));

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("alice");
        assertThat(userCaptor.getValue().getEmail()).isNull();
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        verify(ownershipMigrationService).claimUnownedData(userCaptor.getValue());
        verify(emailVerificationService, never()).verifyAndConsume(any(), any());
    }

    @Test
    @DisplayName("register verifies and binds an optional email to the password account")
    void registerVerifiesAndBindsOptionalEmail() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(emailVerificationService.verifyAndConsume(" Demo@QQ.com ", "123456"))
            .thenReturn("demo@qq.com");
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(
            new AuthRegisterRequest("Alice", "secret123", "Alice", " Demo@QQ.com ", "123456")
        );

        assertThat(response.user().email()).isEqualTo("demo@qq.com");
        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("demo@qq.com");
        verify(emailVerificationService).verifyAndConsume(" Demo@QQ.com ", "123456");
        verify(emailAccountMergeService, never()).retireEmptyGeneratedAccount(any(), any());
    }

    @Test
    @DisplayName("register retires an empty generated email account before binding its email")
    void registerRetiresGeneratedEmailAccount() {
        UserEntity generatedUser = new UserEntity();
        generatedUser.setId(6L);
        generatedUser.setUsername("mail_generated");
        generatedUser.setEmail("demo@qq.com");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(emailVerificationService.verifyAndConsume("demo@qq.com", "123456"))
            .thenReturn("demo@qq.com");
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.of(generatedUser));
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(
            new AuthRegisterRequest("alice", "secret123", "Alice", "demo@qq.com", "123456")
        );

        verify(emailAccountMergeService).retireEmptyGeneratedAccount(generatedUser, "demo@qq.com");
        assertThat(response.user().id()).isEqualTo(7L);
        assertThat(response.user().email()).isEqualTo("demo@qq.com");
    }

    @Test
    @DisplayName("register rejects incomplete optional email verification fields")
    void registerRejectsIncompleteOptionalEmailVerification() {
        assertThatThrownBy(() -> authService.register(
            new AuthRegisterRequest("alice", "secret123", "Alice", "demo@qq.com", null)
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("填写邮箱后必须输入验证码");

        assertThatThrownBy(() -> authService.register(
            new AuthRegisterRequest("alice", "secret123", "Alice", null, "123456")
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("请输入邮箱后再填写验证码");

        verify(emailVerificationService, never()).verifyAndConsume(any(), any());
        verify(userRepository, never()).save(any(UserEntity.class));
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
    @DisplayName("email login reuses an existing email account and issues a normal session")
    void emailLoginReusesExistingAccount() {
        UserEntity user = new UserEntity();
        user.setId(3L);
        user.setUsername("mail_existing");
        user.setEmail("demo@qq.com");
        user.setDisplayName("demo");
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.of(user));
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.loginWithEmail("demo@qq.com");

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().id()).isEqualTo(3L);
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("first email login creates a stable email user without storing a usable password")
    void emailLoginCreatesEmailUser() {
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.empty());
        when(userRepository.count()).thenReturn(1L);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(4L);
            return user;
        });
        when(sessionRepository.save(any(AuthSessionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.loginWithEmail("demo@qq.com");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("demo@qq.com");
        assertThat(saved.getUsername()).startsWith("mail_").hasSize(29);
        assertThat(saved.getDisplayName()).isEqualTo("demo");
        assertThat(saved.getPasswordHash()).isNotBlank();
        assertThat(response.user().id()).isEqualTo(4L);
    }

    @Test
    @DisplayName("binding an unused email keeps the original password account")
    void bindEmailKeepsOriginalAccount() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(2L);
        currentUser.setUsername("alice");
        currentUser.setDisplayName("Alice");
        when(userRepository.findById(2L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.empty());
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        CurrentUserDTO result = authService.bindEmail(2L, "demo@qq.com");

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.email()).isEqualTo("demo@qq.com");
        verify(emailAccountMergeService, never()).retireEmptyGeneratedAccount(any(), any());
    }

    @Test
    @DisplayName("binding retires an empty generated email account before keeping the original account")
    void bindEmailRetiresGeneratedAccount() {
        UserEntity currentUser = new UserEntity();
        currentUser.setId(2L);
        currentUser.setUsername("alice");
        currentUser.setDisplayName("Alice");

        UserEntity generatedUser = new UserEntity();
        generatedUser.setId(6L);
        generatedUser.setUsername("mail_generated");
        generatedUser.setEmail("demo@qq.com");

        when(userRepository.findById(2L)).thenReturn(Optional.of(currentUser));
        when(userRepository.findByEmail("demo@qq.com")).thenReturn(Optional.of(generatedUser));
        when(userRepository.save(currentUser)).thenReturn(currentUser);

        CurrentUserDTO result = authService.bindEmail(2L, "demo@qq.com");

        verify(emailAccountMergeService).retireEmptyGeneratedAccount(generatedUser, "demo@qq.com");
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.email()).isEqualTo("demo@qq.com");
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
