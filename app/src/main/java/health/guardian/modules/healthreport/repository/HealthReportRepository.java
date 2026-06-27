package health.guardian.modules.healthreport.repository;

import health.guardian.modules.healthreport.model.HealthReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * 体检报告Repository
 */
@Repository
public interface HealthReportRepository extends JpaRepository<HealthReportEntity, Long> {
    
    /**
     * 根据文件哈希查找体检报告（用于去重）
     */
    Optional<HealthReportEntity> findByFileHash(String fileHash);

    Optional<HealthReportEntity> findByOwner_IdAndFileHash(Long ownerId, String fileHash);

    Optional<HealthReportEntity> findByIdAndOwner_Id(Long id, Long ownerId);

    List<HealthReportEntity> findByOwner_IdOrderByUploadedAtDesc(Long ownerId);

    List<HealthReportEntity> findByOwnerIsNull();
    
    /**
     * 检查文件哈希是否存在
     */
    boolean existsByFileHash(String fileHash);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);
}
