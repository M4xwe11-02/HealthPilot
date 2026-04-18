package health.guardian.modules.healthreport.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.file.FileStorageService;
import health.guardian.modules.consultation.service.ConsultationPersistenceService;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 体检报告删除服务
 * 处理体检报告删除的业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportDeleteService {
    
    private final HealthReportPersistenceService persistenceService;
    private final ConsultationPersistenceService consultationPersistenceService;
    private final FileStorageService storageService;
    
    /**
     * 删除体检报告
     * 
     * @param id 体检报告ID
     * @throws health.guardian.common.exception.BusinessException 如果体检报告不存在
     */
    public void deleteHealthReport(Long id) {
        log.info("收到删除体检报告请求: id={}", id);
        
        // 获取体检报告信息（用于删除存储文件）
        HealthReportEntity healthReport = persistenceService.findById(id)
            .orElseThrow(() -> new BusinessException(
                ErrorCode.HEALTH_REPORT_NOT_FOUND));
        
        // 1. 删除存储的文件（FileStorageService 已内置存在性检查）
        try {
            storageService.deleteHealthReport(healthReport.getStorageKey());
        } catch (Exception e) {
            log.warn("删除存储文件失败，继续删除数据库记录: {}", e.getMessage());
        }
        
        // 2. 删除问诊会话（会自动删除问诊答案）
        consultationPersistenceService.deleteSessionsByHealthReportId(id);
        
        // 3. 删除数据库记录（包括分析记录）
        persistenceService.deleteHealthReport(id);
        
        log.info("体检报告删除完成: id={}", id);
    }
}

