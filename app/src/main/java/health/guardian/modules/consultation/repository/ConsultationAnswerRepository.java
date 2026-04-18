package health.guardian.modules.consultation.repository;

import health.guardian.modules.consultation.model.ConsultationAnswerEntity;
import health.guardian.modules.consultation.model.ConsultationSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 问诊答案Repository
 */
@Repository
public interface ConsultationAnswerRepository extends JpaRepository<ConsultationAnswerEntity, Long> {
    
    /**
     * 根据会话查找所有答案
     */
    List<ConsultationAnswerEntity> findBySessionOrderByQuestionIndex(ConsultationSessionEntity session);
    
    /**
     * 根据会话ID查找所有答案
     */
    List<ConsultationAnswerEntity> findBySessionIdOrderByQuestionIndex(Long sessionId);
    
    /**
     * 根据会话 sessionId 字符串查找所有答案
     */
    List<ConsultationAnswerEntity> findBySession_SessionIdOrderByQuestionIndex(String sessionId);

    /**
     * 根据会话 sessionId 和问题索引查找单条答案（用于 upsert）
     */
    Optional<ConsultationAnswerEntity> findBySession_SessionIdAndQuestionIndex(String sessionId, Integer questionIndex);
}
