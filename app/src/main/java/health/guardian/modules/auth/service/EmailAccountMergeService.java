package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.UserEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailAccountMergeService {

    private final EntityManager entityManager;
    private final AuthTokenService tokenService;

    public void retireEmptyGeneratedAccount(UserEntity source, String email) {
        if (!isGeneratedEmailAccount(source, email)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该邮箱已绑定其他账号");
        }
        if (hasBusinessData(source.getId())) {
            throw new BusinessException(
                ErrorCode.BAD_REQUEST,
                "该邮箱账号已有业务数据，暂不能自动合并，请先使用邮箱账号登录"
            );
        }

        UserEntity managedSource = entityManager.contains(source) ? source : entityManager.merge(source);
        managedSource.setEmail(null);
        entityManager.flush();

        entityManager.createNativeQuery("DELETE FROM auth_sessions WHERE user_id = :sourceId")
            .setParameter("sourceId", source.getId())
            .executeUpdate();
        entityManager.remove(managedSource);
        entityManager.flush();
    }

    boolean isGeneratedEmailAccount(UserEntity user, String email) {
        if (user == null || user.getEmail() == null || !user.getEmail().equals(email)) {
            return false;
        }
        String identityHash = tokenService.hashToken(email);
        String username = user.getUsername();
        return username.equals("mail_" + identityHash.substring(0, 24))
            || username.equals("mail_" + identityHash.substring(0, 40));
    }

    private boolean hasBusinessData(Long userId) {
        Number count = (Number) entityManager.createNativeQuery("""
            SELECT
                (SELECT COUNT(*) FROM health_reports WHERE owner_id = :userId)
              + (SELECT COUNT(*) FROM consultation_sessions WHERE owner_id = :userId)
              + (SELECT COUNT(*) FROM knowledge_bases WHERE owner_id = :userId)
              + (SELECT COUNT(*) FROM rag_chat_sessions WHERE owner_id = :userId)
            """)
            .setParameter("userId", userId)
            .getSingleResult();
        return count.longValue() > 0;
    }
}
