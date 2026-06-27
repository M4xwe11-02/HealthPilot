package health.guardian.modules.healthreport.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.file.FileHashService;
import health.guardian.infrastructure.mapper.HealthReportMapper;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.healthreport.model.HealthReportAnalysisEntity;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.repository.HealthReportAnalysisRepository;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 体检报告持久化服务
 * 体检报告和健康分析结果的持久化，报告删除时删除所有关联数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportPersistenceService {

    private final HealthReportRepository healthReportRepository;
    private final HealthReportAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper;
    private final HealthReportMapper healthReportMapper;
    private final FileHashService fileHashService;
    private final CurrentUserService currentUserService;
    
    /**
     * 检查体检报告是否已存在（基于文件内容hash）
     * 
     * @param file 上传的文件
     * @return 如果存在返回已有的体检报告实体，否则返回空
     */
    public Optional<HealthReportEntity> findExistingHealthReport(MultipartFile file) {
        try {
            String fileHash = fileHashService.calculateHash(file);
            Long ownerId = currentUserService.requireCurrentUserId();
            Optional<HealthReportEntity> existing = healthReportRepository.findByOwner_IdAndFileHash(ownerId, fileHash);
            
            if (existing.isPresent()) {
                log.info("检测到重复体检报告: hash={}", fileHash);
                HealthReportEntity healthReport = existing.get();
                healthReport.incrementAccessCount();
                healthReportRepository.save(healthReport);
            }
            
            return existing;
        } catch (Exception e) {
            log.error("检查体检报告重复时出错: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * 保存新体检报告
     */
    @Transactional(rollbackFor = Exception.class)
    public HealthReportEntity saveHealthReport(MultipartFile file, String reportText,
                                   String storageKey, String storageUrl) {
        try {
            String fileHash = fileHashService.calculateHash(file);
            
            HealthReportEntity healthReport = new HealthReportEntity();
            healthReport.setOwner(currentUserService.requireCurrentUserReference());
            healthReport.setFileHash(fileHash);
            healthReport.setOriginalFilename(file.getOriginalFilename());
            healthReport.setFileSize(file.getSize());
            healthReport.setContentType(file.getContentType());
            healthReport.setStorageKey(storageKey);
            healthReport.setStorageUrl(storageUrl);
            healthReport.setReportText(reportText);
            
            HealthReportEntity saved = healthReportRepository.save(healthReport);
            log.info("体检报告已保存: id={}, hash={}", saved.getId(), fileHash);
            
            return saved;
        } catch (Exception e) {
            log.error("保存体检报告失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.HEALTH_REPORT_UPLOAD_FAILED, "保存体检报告失败");
        }
    }
    
    /**
     * 保存健康分析结果
     */
    @Transactional(rollbackFor = Exception.class)
    public HealthReportAnalysisEntity saveAnalysis(HealthReportEntity healthReport, HealthReportAnalysisResponse analysis) {
        try {
            // 使用 MapStruct 映射基础字段
            HealthReportAnalysisEntity entity = healthReportMapper.toAnalysisEntity(analysis);
            entity.setHealthReport(healthReport);

            // JSON 字段需要手动序列化
            entity.setStrengthsJson(objectMapper.writeValueAsString(analysis.strengths()));
            entity.setSuggestionsJson(objectMapper.writeValueAsString(analysis.suggestions()));

            HealthReportAnalysisEntity saved = analysisRepository.save(entity);
            log.info("健康分析结果已保存: analysisId={}, healthReportId={}, score={}",
                    saved.getId(), healthReport.getId(), analysis.overallScore());

            return saved;
        } catch (JacksonException e) {
            log.error("序列化评测结果失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.HEALTH_REPORT_ANALYSIS_FAILED, "保存评测结果失败");
        }
    }
    
    /**
     * 获取体检报告的最新健康分析结果
     */
    public Optional<HealthReportAnalysisEntity> getLatestAnalysis(Long healthReportId) {
        return Optional.ofNullable(analysisRepository.findFirstByHealthReportIdOrderByAnalyzedAtDesc(healthReportId));
    }
    
    /**
     * 获取体检报告的最新健康分析结果（返回DTO）
     */
    public Optional<HealthReportAnalysisResponse> getLatestAnalysisAsDTO(Long healthReportId) {
        return getLatestAnalysis(healthReportId).map(this::entityToDTO);
    }
    
    /**
     * 获取所有体检报告列表
     */
    public List<HealthReportEntity> findAllHealthReports() {
        return healthReportRepository.findByOwner_IdOrderByUploadedAtDesc(currentUserService.requireCurrentUserId());
    }
    
    /**
     * 获取体检报告的所有健康分析记录
     */
    public List<HealthReportAnalysisEntity> findAnalysesByHealthReportId(Long healthReportId) {
        return analysisRepository.findByHealthReportIdOrderByAnalyzedAtDesc(healthReportId);
    }
    
    /**
     * 将实体转换为DTO
     */
    public HealthReportAnalysisResponse entityToDTO(HealthReportAnalysisEntity entity) {
        try {
            List<String> strengths = objectMapper.readValue(
                entity.getStrengthsJson() != null ? entity.getStrengthsJson() : "[]",
                    new TypeReference<>() {
                    }
            );
            
            List<HealthReportAnalysisResponse.Suggestion> suggestions = objectMapper.readValue(
                entity.getSuggestionsJson() != null ? entity.getSuggestionsJson() : "[]",
                    new TypeReference<>() {
                    }
            );
            
            return new HealthReportAnalysisResponse(
                entity.getOverallScore(),
                healthReportMapper.toScoreDetail(entity),  // 使用MapStruct自动映射
                entity.getSummary(),
                strengths,
                suggestions,
                entity.getHealthReport().getReportText()
            );
        } catch (JacksonException e) {
            log.error("反序列化评测结果失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.HEALTH_REPORT_ANALYSIS_FAILED, "获取评测结果失败");
        }
    }
    
    /**
     * 根据ID获取体检报告
     */
    public Optional<HealthReportEntity> findById(Long id) {
        return healthReportRepository.findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId());
    }
    
    /**
     * 删除体检报告及其所有关联数据
     * 包括：健康分析记录、问诊会话（会自动删除问诊答案）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteHealthReport(Long id) {
        Optional<HealthReportEntity> healthReportOpt = healthReportRepository.findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId());
        if (healthReportOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND);
        }
        
        HealthReportEntity healthReport = healthReportOpt.get();
        
        // 1. 删除所有健康分析记录
        List<HealthReportAnalysisEntity> analyses = analysisRepository.findByHealthReportIdOrderByAnalyzedAtDesc(id);
        if (!analyses.isEmpty()) {
            analysisRepository.deleteAll(analyses);
            log.info("已删除 {} 条健康分析记录", analyses.size());
        }
        
        // 2. 删除体检报告实体（问诊会话会在服务层删除）
        healthReportRepository.delete(healthReport);
        log.info("体检报告已删除: id={}, filename={}", id, healthReport.getOriginalFilename());
    }
}
