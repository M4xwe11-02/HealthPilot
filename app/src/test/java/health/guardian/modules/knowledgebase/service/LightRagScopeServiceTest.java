package health.guardian.modules.knowledgebase.service;

import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("LightRAG knowledge-base scope")
class LightRagScopeServiceTest {

    private final LightRagScopeService scopeService = new LightRagScopeService();

    @Test
    @DisplayName("workspace includes both owner and knowledge-base ids")
    void buildsPerKnowledgeBaseWorkspace() {
        KnowledgeBaseEntity knowledgeBase = knowledgeBase(7L, 42L, "doc.txt");

        assertEquals("user_7_kb_42", scopeService.workspaceFor(knowledgeBase));
    }

    @Test
    @DisplayName("allowed references use the stable namespaced source")
    void buildsAllowedReferenceSources() {
        KnowledgeBaseEntity knowledgeBase = knowledgeBase(7L, 42L, "doc.txt");

        assertEquals(
            List.of("knowledge-base/42/doc.txt"),
            scopeService.allowedReferenceSources(List.of(knowledgeBase))
        );
    }

    private KnowledgeBaseEntity knowledgeBase(Long ownerId, Long knowledgeBaseId, String filename) {
        UserEntity owner = new UserEntity();
        owner.setId(ownerId);
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId(knowledgeBaseId);
        knowledgeBase.setOwner(owner);
        knowledgeBase.setOriginalFilename(filename);
        return knowledgeBase;
    }
}
