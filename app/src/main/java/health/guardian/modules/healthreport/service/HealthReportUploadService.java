package health.guardian.modules.healthreport.service;

import health.guardian.common.config.AppConfigProperties;
import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.infrastructure.file.FileStorageService;
import health.guardian.infrastructure.file.FileValidationService;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.healthreport.listener.AnalyzeStreamProducer;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.repository.HealthReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * 体检报告上传服务
 * 处理体检报告上传、解析的业务逻辑
 * AI 分析改为异步处理，通过 Redis Stream 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportUploadService {

    private final HealthReportParseService parseService;
    private final FileStorageService storageService;
    private final HealthReportPersistenceService persistenceService;
    private final AppConfigProperties appConfig;
    private final FileValidationService fileValidationService;
    private final AnalyzeStreamProducer analyzeStreamProducer;
    private final HealthReportRepository healthReportRepository;
    private final HealthReportValidationService validationService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 上传并分析体检报告（异步）
     *
     * @param file 体检报告文件
     * @return 上传结果（分析将异步进行）
     */
    public Map<String, Object> uploadAndAnalyze(org.springframework.web.multipart.MultipartFile file) {
        // 1. 验证文件
        fileValidationService.validateFile(file, MAX_FILE_SIZE, "体检报告");

        String fileName = file.getOriginalFilename();
        log.info("收到体检报告上传请求: {}, 大小: {} bytes", fileName, file.getSize());

        // 2. 验证文件类型
        String contentType = parseService.detectContentType(file);
        validateContentType(contentType);

        // 3. 检查体检报告是否已存在（去重）
        Optional<HealthReportEntity> existingHealthReport = persistenceService.findExistingHealthReport(file);
        if (existingHealthReport.isPresent()) {
            return handleDuplicateHealthReport(existingHealthReport.get());
        }

        // 4. 解析体检报告文本
        String reportText = parseService.parseHealthReport(file);
        if (reportText == null || reportText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_PARSE_FAILED, "无法从文件中提取文本内容，请确保文件不是扫描版PDF");
        }

        // 4.5 同步 AI 校验：确认文档为体检报告（fail-open：校验服务故障时放行）
        validationService.validateIsHealthReport(reportText);

        // 5. 保存体检报告到RustFS
        String fileKey = storageService.uploadHealthReport(file);
        String fileUrl = storageService.getFileUrl(fileKey);
        log.info("体检报告已存储到RustFS: {}", fileKey);

        // 6. 保存体检报告到数据库（状态为 PENDING）
        HealthReportEntity savedHealthReport = persistenceService.saveHealthReport(file, reportText, fileKey, fileUrl);

        // 7. 发送分析任务到 Redis Stream（异步处理）
        analyzeStreamProducer.sendAnalyzeTask(savedHealthReport.getId(), reportText);

        log.info("体检报告上传完成，分析任务已入队: {}, healthReportId={}", fileName, savedHealthReport.getId());

        // 8. 返回结果（状态为 PENDING，前端可轮询获取最新状态）
        return Map.of(
            "healthReport", Map.of(
                "id", savedHealthReport.getId(),
                "filename", savedHealthReport.getOriginalFilename(),
                "analyzeStatus", AsyncTaskStatus.PENDING.name()
            ),
            "storage", Map.of(
                "fileKey", fileKey,
                "fileUrl", fileUrl,
                "healthReportId", savedHealthReport.getId()
            ),
            "duplicate", false
        );
    }

    /**
     * 验证文件类型
     */
    private void validateContentType(String contentType) {
        fileValidationService.validateContentTypeByList(
            contentType,
            appConfig.getAllowedTypes(),
            "不支持的文件类型: " + contentType
        );
    }

    /**
     * 处理重复体检报告
     */
    private Map<String, Object> handleDuplicateHealthReport(HealthReportEntity healthReport) {
        log.info("检测到重复体检报告，返回历史分析结果: healthReportId={}", healthReport.getId());

        // 获取历史分析结果
        Optional<HealthReportAnalysisResponse> analysisOpt = persistenceService.getLatestAnalysisAsDTO(healthReport.getId());

        // 已有分析结果，直接返回
        // 没有分析结果（可能之前分析失败），返回当前状态
        return analysisOpt.map(healthReportAnalysisResponse -> Map.of(
                "analysis", healthReportAnalysisResponse,
                "storage", Map.of(
                        "fileKey", healthReport.getStorageKey() != null ? healthReport.getStorageKey() : "",
                        "fileUrl", healthReport.getStorageUrl() != null ? healthReport.getStorageUrl() : "",
                        "healthReportId", healthReport.getId()
                ),
                "duplicate", true
        )).orElseGet(() -> Map.of(
                "healthReport", Map.of(
                        "id", healthReport.getId(),
                        "filename", healthReport.getOriginalFilename(),
                        "analyzeStatus", healthReport.getAnalyzeStatus() != null ? healthReport.getAnalyzeStatus().name() : AsyncTaskStatus.PENDING.name()
                ),
                "storage", Map.of(
                        "fileKey", healthReport.getStorageKey() != null ? healthReport.getStorageKey() : "",
                        "fileUrl", healthReport.getStorageUrl() != null ? healthReport.getStorageUrl() : "",
                        "healthReportId", healthReport.getId()
                ),
                "duplicate", true
        ));
    }

    /**
     * 重新分析体检报告（手动重试）
     * 从数据库获取体检报告文本并发送分析任务
     *
     * @param healthReportId 体检报告ID
     */
    @Transactional
    public void reanalyze(Long healthReportId) {
        HealthReportEntity healthReport = persistenceService.findById(healthReportId)
            .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND, "体检报告不存在"));

        log.info("开始重新分析体检报告: healthReportId={}, filename={}", healthReportId, healthReport.getOriginalFilename());

        String reportText = healthReport.getReportText();
        if (reportText == null || reportText.trim().isEmpty()) {
            // 如果没有缓存的文本，尝试重新解析
            reportText = parseService.downloadAndParseContent(healthReport.getStorageKey(), healthReport.getOriginalFilename());
            if (reportText == null || reportText.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.HEALTH_REPORT_PARSE_FAILED, "无法获取体检报告文本内容");
            }
            // 更新缓存的文本
            healthReport.setReportText(reportText);
        }

        // 更新状态为 PENDING
        healthReport.setAnalyzeStatus(AsyncTaskStatus.PENDING);
        healthReport.setAnalyzeError(null);
        healthReportRepository.save(healthReport);

        // 发送分析任务到 Stream
        analyzeStreamProducer.sendAnalyzeTask(healthReportId, reportText);

        log.info("重新分析任务已发送: healthReportId={}", healthReportId);
    }
}
