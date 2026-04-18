package health.guardian.modules.healthreport.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.export.PdfExportService;
import health.guardian.infrastructure.mapper.ConsultationMapper;
import health.guardian.infrastructure.mapper.HealthReportMapper;
import health.guardian.modules.consultation.model.HealthReportAnalysisResponse;
import health.guardian.modules.consultation.service.ConsultationPersistenceService;
import health.guardian.modules.healthreport.model.HealthReportAnalysisEntity;
import health.guardian.modules.healthreport.model.HealthReportDetailDTO;
import health.guardian.modules.healthreport.model.HealthReportEntity;
import health.guardian.modules.healthreport.model.HealthReportListItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 体检报告历史服务
 * 体检报告历史和导出健康分析报告
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthReportHistoryService {

    private final HealthReportPersistenceService healthReportPersistenceService;
    private final ConsultationPersistenceService consultationPersistenceService;
    private final PdfExportService pdfExportService;
    private final ObjectMapper objectMapper;
    private final HealthReportMapper healthReportMapper;
    private final ConsultationMapper consultationMapper;

    /**
     * 获取所有体检报告列表
     */
    public List<HealthReportListItemDTO> getAllHealthReports() {
        List<HealthReportEntity> healthReports = healthReportPersistenceService.findAllHealthReports();

        return healthReports.stream().map(healthReport -> {
            // 获取最新分析结果的分数
            Integer latestScore = null;
            java.time.LocalDateTime lastAnalyzedAt = null;
            Optional<HealthReportAnalysisEntity> analysisOpt = healthReportPersistenceService.getLatestAnalysis(healthReport.getId());
            if (analysisOpt.isPresent()) {
                HealthReportAnalysisEntity analysis = analysisOpt.get();
                latestScore = analysis.getOverallScore();
                lastAnalyzedAt = analysis.getAnalyzedAt();
            }

            // 获取问诊次数
            int consultationCount = consultationPersistenceService.findByHealthReportId(healthReport.getId()).size();

            // 使用 MapStruct 映射
            return new HealthReportListItemDTO(
                healthReport.getId(),
                healthReport.getOriginalFilename(),
                healthReport.getFileSize(),
                healthReport.getUploadedAt(),
                healthReport.getAccessCount(),
                latestScore,
                lastAnalyzedAt,
                consultationCount
            );
        }).toList();
    }

    /**
     * 获取体检报告详情（包含分析历史）
     */
    public HealthReportDetailDTO getHealthReportDetail(Long id) {
        Optional<HealthReportEntity> healthReportOpt = healthReportPersistenceService.findById(id);
        if (healthReportOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND);
        }

        HealthReportEntity healthReport = healthReportOpt.get();

        // 获取所有分析记录，使用 MapStruct 批量转换
        List<HealthReportAnalysisEntity> analyses = healthReportPersistenceService.findAnalysesByHealthReportId(id);
        List<HealthReportDetailDTO.AnalysisHistoryDTO> analysisHistory = healthReportMapper.toAnalysisHistoryDTOList(
            analyses,
            this::extractStrengths,
            this::extractSuggestions
        );

        // 使用 ConsultationMapper 转换问诊历史
        List<Object> consultationHistory = consultationMapper.toConsultationHistoryList(
            consultationPersistenceService.findByHealthReportId(id)
        );

        return new HealthReportDetailDTO(
            healthReport.getId(),
            healthReport.getOriginalFilename(),
            healthReport.getFileSize(),
            healthReport.getContentType(),
            healthReport.getStorageUrl(),
            healthReport.getUploadedAt(),
            healthReport.getAccessCount(),
            healthReport.getReportText(),
            healthReport.getAnalyzeStatus(),
            healthReport.getAnalyzeError(),
            analysisHistory,
            consultationHistory
        );
    }

    /**
     * 从 JSON 提取 strengths
     */
    private List<String> extractStrengths(HealthReportAnalysisEntity entity) {
        try {
            if (entity.getStrengthsJson() != null) {
                return objectMapper.readValue(
                    entity.getStrengthsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 strengths JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 从 JSON 提取 suggestions
     */
    private List<Object> extractSuggestions(HealthReportAnalysisEntity entity) {
        try {
            if (entity.getSuggestionsJson() != null) {
                return objectMapper.readValue(
                    entity.getSuggestionsJson(),
                        new TypeReference<>() {
                        }
                );
            }
        } catch (JacksonException e) {
            log.error("解析 suggestions JSON 失败", e);
        }
        return List.of();
    }

    /**
     * 导出健康分析报告为PDF
     */
    public ExportResult exportAnalysisPdf(Long healthReportId) {
        Optional<HealthReportEntity> healthReportOpt = healthReportPersistenceService.findById(healthReportId);
        if (healthReportOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_NOT_FOUND);
        }

        HealthReportEntity healthReport = healthReportOpt.get();
        Optional<HealthReportAnalysisResponse> analysisOpt = healthReportPersistenceService.getLatestAnalysisAsDTO(healthReportId);
        if (analysisOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.HEALTH_REPORT_ANALYSIS_NOT_FOUND);
        }

        try {
            byte[] pdfBytes = pdfExportService.exportHealthReportAnalysis(healthReport, analysisOpt.get());
            String filename = "健康分析报告_" + healthReport.getOriginalFilename() + ".pdf";

            return new ExportResult(pdfBytes, filename);
        } catch (Exception e) {
            log.error("导出PDF失败: healthReportId={}", healthReportId, e);
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "导出PDF失败: " + e.getMessage());
        }
    }

    /**
     * PDF导出结果
     */
    public record ExportResult(byte[] pdfBytes, String filename) {}
}

