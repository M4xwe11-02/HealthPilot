package health.guardian.modules.knowledgebase.service;

import health.guardian.common.config.LightRagProperties;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Re-ingests legacy shared-workspace documents into their owner's isolated workspace.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightRagMigrationService {

    private final LightRagProperties properties;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseParseService parseService;
    private final LightRagDocumentService documentService;
    private final Set<Long> migrationsInProgress = ConcurrentHashMap.newKeySet();

    public void migrateAsync(List<KnowledgeBaseEntity> knowledgeBases) {
        if (!properties.isEnabled() || !properties.isIngestOnUpload() || knowledgeBases == null) {
            return;
        }
        knowledgeBases.stream()
            .filter(knowledgeBase -> knowledgeBase != null)
            .map(KnowledgeBaseEntity::getId)
            .filter(id -> id != null && migrationsInProgress.add(id))
            .forEach(this::startMigration);
    }

    private void startMigration(Long knowledgeBaseId) {
        Thread.ofVirtual().name("lightrag-migrate-", 0).start(() -> {
            try {
                KnowledgeBaseEntity knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                    .orElse(null);
                if (knowledgeBase == null) {
                    return;
                }
                String content = parseService.downloadAndParseContent(
                    knowledgeBase.getStorageKey(),
                    knowledgeBase.getOriginalFilename()
                );
                if (content == null || content.isBlank()) {
                    log.warn("LightRAG workspace migration skipped because content is blank: kbId={}", knowledgeBaseId);
                    return;
                }
                documentService.submitTextAsync(knowledgeBase, content);
                log.info("LightRAG workspace migration submitted: kbId={}", knowledgeBaseId);
            } catch (Exception error) {
                log.warn("LightRAG workspace migration failed: kbId={}, error={}",
                    knowledgeBaseId, error.getMessage(), error);
            } finally {
                migrationsInProgress.remove(knowledgeBaseId);
            }
        });
    }
}
