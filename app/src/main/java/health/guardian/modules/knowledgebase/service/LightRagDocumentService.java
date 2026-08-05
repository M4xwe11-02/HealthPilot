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

    private static final int TRACK_POLL_ATTEMPTS = 360;
    private static final long TRACK_POLL_INTERVAL_MILLIS = 5_000L;

    private final LightRagProperties properties;
    private final LightRagClient lightRagClient;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final LightRagScopeService lightRagScopeService;

    public boolean submitTextAsync(KnowledgeBaseEntity knowledgeBase, String content) {
        if (!properties.isEnabled() || !properties.isIngestOnUpload()) {
            updateStatus(knowledgeBase.getId(), "NOT_SUBMITTED", null, null, null);
            return false;
        }
        if (content == null || content.isBlank()) {
            updateStatus(knowledgeBase.getId(), "FAILED", null, "content is blank", null);
            return false;
        }

        Long kbId = knowledgeBase.getId();
        String workspace = lightRagScopeService.workspaceFor(knowledgeBase);
        String fileSource = lightRagScopeService.fileSourceFor(knowledgeBase);
        updateStatus(kbId, "SUBMITTING", null, null, workspace);

        Thread.ofVirtual().name("lightrag-ingest-", 0).start(() -> {
            try {
                LightRagClient.LightRagInsertResult result = lightRagClient.insertText(content, fileSource, workspace);
                updateStatus(kbId, "PROCESSING", result.trackId(), null, workspace);
                log.info("LightRAG ingestion submitted: kbId={}, workspace={}, trackId={}, status={}",
                    kbId, workspace, result.trackId(), result.status());
                waitForProcessing(kbId, result.trackId(), workspace);
            } catch (Exception e) {
                updateStatus(kbId, "FAILED", null, e.getMessage(), workspace);
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
            String workspace = lightRagScopeService.workspaceFor(knowledgeBase);
            if (!workspace.equals(knowledgeBase.getLightRagWorkspace())) {
                return currentStatus;
            }
            LightRagClient.LightRagTrackStatus status = lightRagClient.getTrackStatus(trackId, workspace);
            updateStatus(knowledgeBase.getId(), status.status(), trackId, status.error(), workspace);
            return status.status();
        } catch (Exception e) {
            log.warn("LightRAG status refresh failed: kbId={}, trackId={}, error={}",
                knowledgeBase.getId(), trackId, e.getMessage());
            return currentStatus;
        }
    }

    public boolean deleteTrackedDocument(KnowledgeBaseEntity knowledgeBase) {
        if (!properties.isEnabled()) {
            return false;
        }

        String trackId = knowledgeBase.getLightRagTrackId();
        if (trackId == null || trackId.isBlank()) {
            log.debug("跳过 LightRAG 删除：知识库没有 trackId, kbId={}", knowledgeBase.getId());
            return false;
        }

        String workspace = lightRagScopeService.workspaceFor(knowledgeBase);
        if (!workspace.equals(knowledgeBase.getLightRagWorkspace())) {
            log.info("跳过旧共享 workspace 中的 LightRAG 文档删除: kbId={}, recordedWorkspace={}",
                knowledgeBase.getId(), knowledgeBase.getLightRagWorkspace());
            return false;
        }
        LightRagClient.LightRagTrackStatus status = lightRagClient.getTrackStatus(trackId, workspace);
        if (status.docIds().isEmpty()) {
            log.warn("LightRAG 删除跳过：trackId 未找到文档, kbId={}, trackId={}", knowledgeBase.getId(), trackId);
            return false;
        }

        LightRagClient.LightRagDeleteResult result = lightRagClient.deleteDocuments(status.docIds(), workspace);
        log.info("LightRAG 文档删除已提交: kbId={}, workspace={}, trackId={}, docIds={}, status={}, message={}",
            knowledgeBase.getId(), workspace, trackId, status.docIds(), result.status(), result.message());
        return true;
    }

    private void waitForProcessing(Long kbId, String trackId, String workspace) throws InterruptedException {
        if (trackId == null || trackId.isBlank()) {
            return;
        }

        for (int i = 0; i < TRACK_POLL_ATTEMPTS; i++) {
            LightRagClient.LightRagTrackStatus status = lightRagClient.getTrackStatus(trackId, workspace);
            updateStatus(kbId, status.status(), trackId, status.error(), workspace);
            if ("COMPLETED".equals(status.status()) || "FAILED".equals(status.status())) {
                log.info("LightRAG ingestion finished: kbId={}, workspace={}, trackId={}, status={}",
                    kbId, workspace, trackId, status.status());
                return;
            }
            Thread.sleep(TRACK_POLL_INTERVAL_MILLIS);
        }

        updateStatus(kbId, "PROCESSING", trackId, "LightRAG processing timeout; check pipeline status", workspace);
        log.warn("LightRAG ingestion still processing after timeout: kbId={}, trackId={}", kbId, trackId);
    }

    private void updateStatus(Long kbId, String status, String trackId, String error, String workspace) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setLightRagStatus(normalizeStatus(status));
            if (trackId != null) {
                kb.setLightRagTrackId(trackId);
            }
            if (workspace != null && !workspace.isBlank()) {
                kb.setLightRagWorkspace(workspace);
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
