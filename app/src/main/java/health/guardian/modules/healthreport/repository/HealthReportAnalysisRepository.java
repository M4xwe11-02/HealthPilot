package health.guardian.modules.healthreport.repository;

import health.guardian.modules.healthreport.model.HealthReportAnalysisEntity;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 健康分析Repository
 */
@Repository
public interface HealthReportAnalysisRepository extends JpaRepository<HealthReportAnalysisEntity, Long> {
    
    /**
     * 根据体检报告查找所有健康分析记录
     */
    List<HealthReportAnalysisEntity> findByHealthReportOrderByAnalyzedAtDesc(HealthReportEntity healthReport);
    
    /**
     * 根据体检报告ID查找最新健康分析记录
     */
    HealthReportAnalysisEntity findFirstByHealthReportIdOrderByAnalyzedAtDesc(Long healthReportId);
    
    /**
     * 根据体检报告ID查找所有健康分析记录
     */
    List<HealthReportAnalysisEntity> findByHealthReportIdOrderByAnalyzedAtDesc(Long healthReportId);
}
