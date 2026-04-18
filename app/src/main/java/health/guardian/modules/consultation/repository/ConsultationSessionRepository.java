package health.guardian.modules.consultation.repository;

import health.guardian.modules.consultation.model.ConsultationSessionEntity;
import health.guardian.modules.consultation.model.ConsultationSessionEntity.SessionStatus;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 问诊会话Repository
 */
@Repository
public interface ConsultationSessionRepository extends JpaRepository<ConsultationSessionEntity, Long> {

    /**
     * 根据会话ID查找
     */
    Optional<ConsultationSessionEntity> findBySessionId(String sessionId);

    Optional<ConsultationSessionEntity> findBySessionIdAndOwner_Id(String sessionId, Long ownerId);

    /**
     * 根据会话ID查找（同时加载关联的体检报告）
     */
    @Query("SELECT s FROM ConsultationSessionEntity s JOIN FETCH s.healthReport WHERE s.sessionId = :sessionId")
    Optional<ConsultationSessionEntity> findBySessionIdWithHealthReport(@Param("sessionId") String sessionId);

    @Query("SELECT s FROM ConsultationSessionEntity s JOIN FETCH s.healthReport WHERE s.sessionId = :sessionId AND s.owner.id = :ownerId")
    Optional<ConsultationSessionEntity> findBySessionIdWithHealthReportAndOwner(@Param("sessionId") String sessionId, @Param("ownerId") Long ownerId);
    
    /**
     * 根据体检报告查找所有问诊记录
     */
    List<ConsultationSessionEntity> findByHealthReportOrderByCreatedAtDesc(HealthReportEntity healthReport);
    
    /**
     * 根据体检报告ID查找所有问诊记录
     */
    List<ConsultationSessionEntity> findByHealthReportIdOrderByCreatedAtDesc(Long healthReportId);

    List<ConsultationSessionEntity> findByHealthReportIdAndOwner_IdOrderByCreatedAtDesc(Long healthReportId, Long ownerId);

    /**
     * 根据体检报告ID查找最近的问诊记录（用于历史题去重）
     */
    List<ConsultationSessionEntity> findTop10ByHealthReportIdOrderByCreatedAtDesc(Long healthReportId);

    List<ConsultationSessionEntity> findTop10ByHealthReportIdAndOwner_IdOrderByCreatedAtDesc(Long healthReportId, Long ownerId);
    
    /**
     * 查找体检报告的未完成问诊（CREATED或IN_PROGRESS状态）
     */
    Optional<ConsultationSessionEntity> findFirstByHealthReportIdAndStatusInOrderByCreatedAtDesc(
        Long healthReportId, 
        List<SessionStatus> statuses
    );

    Optional<ConsultationSessionEntity> findFirstByHealthReportIdAndOwner_IdAndStatusInOrderByCreatedAtDesc(
        Long healthReportId,
        Long ownerId,
        List<SessionStatus> statuses
    );
    
    /**
     * 根据体检报告ID和状态查找会话
     */
    Optional<ConsultationSessionEntity> findByHealthReportIdAndStatusIn(
        Long healthReportId,
        List<SessionStatus> statuses
    );

    boolean existsBySessionIdAndOwner_Id(String sessionId, Long ownerId);

    List<ConsultationSessionEntity> findByOwnerIsNull();
}
