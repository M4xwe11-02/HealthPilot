package health.guardian.modules.knowledgebase.service;

import health.guardian.common.config.LightRagProperties;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LightRAG workspace migration")
class LightRagMigrationServiceTest {

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private KnowledgeBaseParseService parseService;
    @Mock
    private LightRagDocumentService documentService;

    private LightRagMigrationService migrationService;

    @BeforeEach
    void setUp() {
        LightRagProperties properties = new LightRagProperties();
        properties.setEnabled(true);
        properties.setIngestOnUpload(true);
        migrationService = new LightRagMigrationService(
            properties,
            knowledgeBaseRepository,
            parseService,
            documentService
        );
    }

    @Test
    @DisplayName("migration immediately exposes processing state and re-ingests parsed content")
    void marksAndReingestsLegacyKnowledgeBase() {
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId(42L);
        knowledgeBase.setStorageKey("knowledge/42/doc.txt");
        knowledgeBase.setOriginalFilename("doc.txt");
        knowledgeBase.setLightRagStatus("COMPLETED");
        knowledgeBase.setLightRagTrackId("legacy-track");
        knowledgeBase.setLightRagWorkspace("user_7");
        when(knowledgeBaseRepository.save(knowledgeBase)).thenReturn(knowledgeBase);
        when(knowledgeBaseRepository.findById(42L)).thenReturn(Optional.of(knowledgeBase));
        when(parseService.downloadAndParseContent("knowledge/42/doc.txt", "doc.txt"))
            .thenReturn("parsed content");

        migrationService.migrateAsync(List.of(knowledgeBase));

        assertEquals("MIGRATING", knowledgeBase.getLightRagStatus());
        assertNull(knowledgeBase.getLightRagTrackId());
        verify(documentService, timeout(2_000)).submitTextAsync(knowledgeBase, "parsed content");
    }
}
