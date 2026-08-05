package health.guardian.modules.knowledgebase.service;

import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.model.QueryRequest;
import health.guardian.modules.knowledgebase.model.QueryResponse;
import health.guardian.modules.knowledgebase.model.RagProvider;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LightRAG query isolation")
class LightRagQueryServiceIsolationTest {

    @Mock
    private LightRagClient lightRagClient;
    @Mock
    private LightRagDocumentService documentService;
    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private KnowledgeBaseListService listService;
    @Mock
    private KnowledgeBaseCountService countService;
    @Mock
    private LightRagScopeService scopeService;
    @Mock
    private LightRagMigrationService migrationService;

    private LightRagQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new LightRagQueryService(
            lightRagClient,
            documentService,
            knowledgeBaseRepository,
            currentUserService,
            listService,
            countService,
            scopeService,
            migrationService
        );
    }

    @Test
    @DisplayName("rejects selected knowledge bases not owned by the current user")
    void rejectsKnowledgeBasesOutsideCurrentOwner() {
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(42L))).thenReturn(List.of());
        when(listService.getKnowledgeBaseNames(List.of(42L))).thenReturn(List.of());

        QueryResponse response = queryService.queryKnowledgeBase(request());

        assertTrue(response.answer().contains("无权访问"));
        verify(lightRagClient, never()).query(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("legacy shared-workspace records are migrated before any query")
    void migratesLegacyWorkspaceBeforeQuery() {
        KnowledgeBaseEntity knowledgeBase = knowledgeBase(null);
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(42L)))
            .thenReturn(List.of(knowledgeBase));
        when(scopeService.currentUserWorkspace()).thenReturn("user_7");
        when(listService.getKnowledgeBaseNames(List.of(42L))).thenReturn(List.of("private-doc"));

        QueryResponse response = queryService.queryKnowledgeBase(request());

        assertTrue(response.answer().contains("迁移"));
        verify(migrationService).migrateAsync(List.of(knowledgeBase));
        verify(lightRagClient, never()).query(org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("queries only the isolated workspace after migration")
    void queriesCurrentUserWorkspaceAfterMigration() {
        KnowledgeBaseEntity knowledgeBase = knowledgeBase("user_7");
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(42L)))
            .thenReturn(List.of(knowledgeBase));
        when(scopeService.currentUserWorkspace()).thenReturn("user_7");
        when(documentService.refreshTrackedStatus(knowledgeBase)).thenReturn("COMPLETED");
        when(scopeService.allowedSourcePrefixes(List.of(knowledgeBase)))
            .thenReturn(List.of("knowledge-base/42/"));
        when(lightRagClient.query("question", "user_7", List.of("knowledge-base/42/")))
            .thenReturn(new LightRagClient.LightRagQueryResult("isolated answer"));
        when(listService.getKnowledgeBaseNames(List.of(42L))).thenReturn(List.of("private-doc"));

        QueryResponse response = queryService.queryKnowledgeBase(request());

        assertEquals("isolated answer", response.answer());
        verify(lightRagClient).query("question", "user_7", List.of("knowledge-base/42/"));
        verify(migrationService, never()).migrateAsync(org.mockito.ArgumentMatchers.any());
    }

    private QueryRequest request() {
        return new QueryRequest(List.of(42L), "question", RagProvider.LIGHTRAG);
    }

    private KnowledgeBaseEntity knowledgeBase(String workspace) {
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId(42L);
        knowledgeBase.setName("private-doc");
        knowledgeBase.setLightRagStatus("COMPLETED");
        knowledgeBase.setLightRagWorkspace(workspace);
        return knowledgeBase;
    }
}
