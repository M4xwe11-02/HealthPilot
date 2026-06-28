package health.guardian.modules.knowledgebase.service;

import health.guardian.common.config.LightRagProperties;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Submits parsed knowledge-base text to the external LightRAG service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightRagDocumentService {

    private static final int TRACK_POLL_ATTEMPTS = 120;
    private static final long TRACK_POLL_INTERVAL_MILLIS = 5_000L;

    private final LightRagProperties properties;
    private final LightRagClient lightRagClient;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public boolean submitTextAsync(KnowledgeBaseEntity knowledgeBase, String content) {
        if (!properties.isEnabled() || !properties.isIngestOnUpload()) {
            updateStatus(knowledgeBase.getId(), "NOT_SUBMITTED", null, null);
            return false;
        }
        if (content == null || content.isBlank()) {
            updateStatus(knowledgeBase.getId(), "FAILED", null, "content is blank");
            return false;
        }

        Long kbId = knowledgeBase.getId();
        String fileSource = "knowledge-base/" + kbId + "/" + knowledgeBase.getOriginalFilename();
        updateStatus(kbId, "SUBMITTING", null, null);

        Thread.ofVirtual().name("lightrag-ingest-", 0).start(() -> {
            try {
                LightRagClient.LightRagInsertResult result = lightRagClient.insertText(content, fileSource);
                updateStatus(kbId, "PROCESSING", result.trackId(), null);
                log.info("LightRAG ingestion submitted: kbId={}, trackId={}, status={}",
                    kbId, result.trackId(), result.status());
                waitForProcessing(kbId, result.trackId());
            } catch (Exception e) {
                updateStatus(kbId, "FAILED", null, e.getMessage());
                log.warn("LightRAG ingestion failed: kbId={}, error={}", kbId, e.getMessage());
            }
        });
        return true;
    }

    public String refreshTrackedStatus(KnowledgeBaseEntity knowledgeBase) {
        String trackId = knowledgeBase.getLightRagTrackId();
        if (!properties.isEnabled() || trackId == null || trackId.isBlank()) {
            return knowledgeBase.getLightRagStatus();
        }

        String rawStatus = knowledgeBase.getLightRagStatus();
        String currentStatus = normalizeStatus(rawStatus);
        if (isTerminalStatus(rawStatus)) {
            return currentStatus;
        }

        try {
            LightRagClient.LightRagTrackStatus status = lightRagClient.getTrackStatus(trackId);
            updateStatus(knowledgeBase.getId(), status.status(), trackId, status.error());
            return status.status();
        } catch (Exception e) {
            log.warn("LightRAG status refresh failed: kbId={}, trackId={}, error={}",
                knowledgeBase.getId(), trackId, e.getMessage());
            return currentStatus;
        }
    }

    private void waitForProcessing(Long kbId, String trackId) throws InterruptedException {
        if (trackId == null || trackId.isBlank()) {
            return;
        }

        for (int i = 0; i < TRACK_POLL_ATTEMPTS; i++) {
            LightRagClient.LightRagTrackStatus status = lightRagClient.getTrackStatus(trackId);
            updateStatus(kbId, status.status(), trackId, status.error());
            if ("COMPLETED".equals(status.status()) || "FAILED".equals(status.status())) {
                log.info("LightRAG ingestion finished: kbId={}, trackId={}, status={}",
                    kbId, trackId, status.status());
                return;
            }
            Thread.sleep(TRACK_POLL_INTERVAL_MILLIS);
        }

        updateStatus(kbId, "PROCESSING", trackId, "LightRAG processing timeout; check pipeline status");
        log.warn("LightRAG ingestion still processing after timeout: kbId={}, trackId={}", kbId, trackId);
    }

    private void updateStatus(Long kbId, String status, String trackId, String error) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setLightRagStatus(normalizeStatus(status));
            if (trackId != null) {
                kb.setLightRagTrackId(trackId);
            }
            kb.setLightRagError(error);
            knowledgeBaseRepository.save(kb);
        });
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = status.trim().toUpperCase();
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
        }
        return switch (normalized) {
            case "SUCCESS", "PROCESSED" -> "COMPLETED";
            case "PENDING", "SUBMITTED", "SUBMITTING", "PREPROCESSED" -> "PROCESSING";
            default -> normalized;
        };
    }

    private boolean isTerminalStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase();
        if (normalized.contains(".")) {
            normalized = normalized.substring(normalized.lastIndexOf('.') + 1);
        }
        return "COMPLETED".equals(normalized) || "PROCESSED".equals(normalized) || "FAILED".equals(normalized);
    }
}
