package health.guardian.modules.healthreport.model;

import java.time.LocalDateTime;

/**
 * 体检报告列表项DTO
 */
public record HealthReportListItemDTO(
    Long id,
    String filename,
    Long fileSize,
    LocalDateTime uploadedAt,
    Integer accessCount,
    Integer latestScore,
    LocalDateTime lastAnalyzedAt,
    Integer consultationCount
) {}

