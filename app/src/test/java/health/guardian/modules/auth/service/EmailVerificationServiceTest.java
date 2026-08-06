package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.config.EmailAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService")
class EmailVerificationServiceTest {

    @Mock
    private EmailCodeStore codeStore;

    @Mock
    private VerificationMailSender mailSender;

    private EmailAuthProperties properties;
    private PasswordHasher passwordHasher;
    private AuthTokenService tokenService;
    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new EmailAuthProperties();
        properties.setEnabled(true);
        properties.setFrom("sender@qq.com");
        passwordHasher = new PasswordHasher();
        tokenService = new AuthTokenService();
        service = new EmailVerificationService(
            properties,
            codeStore,
            mailSender,
            passwordHasher,
            tokenService
        );
    }

    @Test
    @DisplayName("send stores only a hash and sends the same six digit code")
    void sendStoresHashAndSendsCode() {
        String identity = tokenService.hashToken("demo@qq.com");
        when(codeStore.reserveSend(identity, Duration.ofSeconds(60), Duration.ofHours(1), 5))
            .thenReturn(EmailCodeStore.SendReservation.ACQUIRED);

        service.sendCode(" Demo@QQ.COM ");

        ArgumentCaptor<PasswordHasher.PasswordHash> hashCaptor =
            ArgumentCaptor.forClass(PasswordHasher.PasswordHash.class);
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(codeStore).saveCode(eq(identity), hashCaptor.capture(), eq(Duration.ofMinutes(5)));
        verify(mailSender).sendCode(eq("demo@qq.com"), codeCaptor.capture(), eq(Duration.ofMinutes(5)));

        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(passwordHasher.verify(
            codeCaptor.getValue(),
            hashCaptor.getValue().salt(),
            hashCaptor.getValue().hash()
        )).isTrue();
        assertThat(hashCaptor.getValue().hash()).doesNotContain(codeCaptor.getValue());
    }

    @Test
    @DisplayName("cooldown rejects a resend before generating or sending a code")
    void cooldownRejectsResend() {
        String identity = tokenService.hashToken("demo@qq.com");
        when(codeStore.reserveSend(identity, Duration.ofSeconds(60), Duration.ofHours(1), 5))
            .thenReturn(EmailCodeStore.SendReservation.COOLDOWN);

        assertThatThrownBy(() -> service.sendCode("demo@qq.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.BAD_REQUEST.getCode());

        verify(codeStore, never()).saveCode(any(), any(), any());
        verify(mailSender, never()).sendCode(any(), any(), any());
    }

    @Test
    @DisplayName("mail failure removes the unusable code and releases send limits")
    void mailFailureRollsBackRedisState() {
        String identity = tokenService.hashToken("demo@qq.com");
        when(codeStore.reserveSend(identity, Duration.ofSeconds(60), Duration.ofHours(1), 5))
            .thenReturn(EmailCodeStore.SendReservation.ACQUIRED);
        doThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "send failed"))
            .when(mailSender).sendCode(eq("demo@qq.com"), any(), eq(Duration.ofMinutes(5)));

        assertThatThrownBy(() -> service.sendCode("demo@qq.com"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("send failed");

        verify(codeStore).removeCode(identity);
        verify(codeStore).releaseSend(identity);
    }

    @Test
    @DisplayName("successful verification consumes the normalized email identity")
    void successfulVerificationReturnsNormalizedEmail() {
        String identity = tokenService.hashToken("demo@qq.com");
        when(codeStore.consume(identity, "123456", 5))
            .thenReturn(EmailCodeStore.ConsumeResult.SUCCESS);

        assertThat(service.verifyAndConsume(" Demo@QQ.com ", "123456"))
            .isEqualTo("demo@qq.com");
    }

    @Test
    @DisplayName("missing, invalid, and exhausted codes expose the same generic error")
    void invalidVerificationUsesGenericError() {
        String identity = tokenService.hashToken("demo@qq.com");
        when(codeStore.consume(identity, "123456", 5))
            .thenReturn(EmailCodeStore.ConsumeResult.TOO_MANY_ATTEMPTS);

        assertThatThrownBy(() -> service.verifyAndConsume("demo@qq.com", "123456"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("验证码错误或已过期");
    }
}
