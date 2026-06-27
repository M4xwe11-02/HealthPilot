package health.guardian.modules.publicdoc.model;

import health.guardian.common.model.AsyncTaskStatus;

import java.time.Instant;
import java.util.List;

public record PublicDocDetailDTO(
    Long id,
    String title,
    String category,
    String source,
    String description,
    Long fileSize,
    Instant uploadedAt,
    AsyncTaskStatus analyzeStatus,
    Long downloadCount,
    String textPreview,
    Analysis analysis
) {
    public record Analysis(
        String summary,
        List<String> keyPoints,
        String applicablePopulation,
        List<String> mainRecommendations
    ) {}
}
