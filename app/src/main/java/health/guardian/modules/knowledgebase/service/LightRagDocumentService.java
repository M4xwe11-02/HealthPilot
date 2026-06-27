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
                updateStatus(kbId, result.status(), result.trackId(), null);
                log.info("LightRAG ingestion submitted: kbId={}, trackId={}, status={}",
                    kbId, result.trackId(), result.status());
            } catch (Exception e) {
                updateStatus(kbId, "FAILED", null, e.getMessage());
                log.warn("LightRAG ingestion failed: kbId={}, error={}", kbId, e.getMessage());
            }
        });
        return true;
    }

    private void updateStatus(Long kbId, String status, String trackId, String error) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            kb.setLightRagStatus(status);
            if (trackId != null) {
                kb.setLightRagTrackId(trackId);
            }
            kb.setLightRagError(error);
            knowledgeBaseRepository.save(kb);
        });
    }
}
