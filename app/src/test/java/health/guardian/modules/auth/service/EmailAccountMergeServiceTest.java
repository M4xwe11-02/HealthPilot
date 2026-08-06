package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.modules.auth.model.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailAccountMergeService")
class EmailAccountMergeServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query countQuery;

    @Mock
    private Query deleteSessionsQuery;

    private AuthTokenService tokenService;
    private EmailAccountMergeService service;

    @BeforeEach
    void setUp() {
        tokenService = new AuthTokenService();
        service = new EmailAccountMergeService(entityManager, tokenService);
    }

    @Test
    @DisplayName("empty generated email account is retired after its sessions are revoked")
    void emptyGeneratedAccountIsRetired() {
        UserEntity generated = generatedUser("demo@qq.com");
        when(entityManager.createNativeQuery(contains("SELECT COUNT"))).thenReturn(countQuery);
        when(countQuery.setParameter("userId", 6L)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(entityManager.contains(generated)).thenReturn(true);
        when(entityManager.createNativeQuery(contains("DELETE FROM auth_sessions")))
            .thenReturn(deleteSessionsQuery);
        when(deleteSessionsQuery.setParameter("sourceId", 6L)).thenReturn(deleteSessionsQuery);
        when(deleteSessionsQuery.executeUpdate()).thenReturn(1);

        service.retireEmptyGeneratedAccount(generated, "demo@qq.com");

        assertThat(generated.getEmail()).isNull();
        verify(entityManager).remove(generated);
        verify(entityManager, times(2)).flush();
    }

    @Test
    @DisplayName("generated account with business data is not silently merged")
    void generatedAccountWithBusinessDataIsRejected() {
        UserEntity generated = generatedUser("demo@qq.com");
        when(entityManager.createNativeQuery(contains("SELECT COUNT"))).thenReturn(countQuery);
        when(countQuery.setParameter("userId", 6L)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(1L);

        assertThatThrownBy(() -> service.retireEmptyGeneratedAccount(generated, "demo@qq.com"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("已有业务数据");

        verify(entityManager, never()).remove(generated);
    }

    @Test
    @DisplayName("ordinary password account is never treated as a mergeable email account")
    void ordinaryAccountIsRejected() {
        UserEntity ordinary = new UserEntity();
        ordinary.setId(7L);
        ordinary.setUsername("someone");
        ordinary.setEmail("demo@qq.com");

        assertThatThrownBy(() -> service.retireEmptyGeneratedAccount(ordinary, "demo@qq.com"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("该邮箱已绑定其他账号");

        verify(entityManager, never()).createNativeQuery(anyString());
    }

    private UserEntity generatedUser(String email) {
        UserEntity user = new UserEntity();
        user.setId(6L);
        user.setEmail(email);
        user.setUsername("mail_" + tokenService.hashToken(email).substring(0, 24));
        return user;
    }
}
