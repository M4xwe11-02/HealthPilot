package health.guardian.modules.healthreport.model;

import health.guardian.common.model.AsyncTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 体检报告详情DTO
 */
public record HealthReportDetailDTO(
    Long id,
    String filename,
    Long fileSize,
    String contentType,
    String storageUrl,
    LocalDateTime uploadedAt,
    Integer accessCount,
    String reportText,
    AsyncTaskStatus analyzeStatus,
    String analyzeError,
    List<AnalysisHistoryDTO> analyses,
    List<Object> consultations  // 问诊历史由ConsultationHistoryService提供
) {
    /**
     * 分析历史DTO
     */
    public record AnalysisHistoryDTO(
        Long id,
        Integer overallScore,
        Integer lifestyleScore,
        Integer nutritionScore,
        Integer physicalFitnessScore,
        Integer mentalHealthScore,
        Integer preventiveScore,
        String summary,
        LocalDateTime analyzedAt,
        List<String> strengths,
        List<Object> suggestions
    ) {}
}

