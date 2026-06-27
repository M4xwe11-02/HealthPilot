package health.guardian.modules.publicdoc.model;

import health.guardian.common.model.AsyncTaskStatus;

import java.time.Instant;

public record PublicDocListItemDTO(
    Long id,
    String title,
    String category,
    String source,
    String description,
    Long fileSize,
    Instant uploadedAt,
    AsyncTaskStatus analyzeStatus,
    Long downloadCount
) {
    public static PublicDocListItemDTO from(PublicDocEntity e) {
        return new PublicDocListItemDTO(
            e.getId(),
            e.getTitle(),
            e.getCategory(),
            e.getSource(),
            e.getDescription(),
            e.getFileSize(),
            e.getUploadedAt(),
            e.getAnalyzeStatus(),
            e.getDownloadCount()
        );
    }
}
