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
import reactor.core.publisher.Flux;

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
        when(scopeService.workspaceFor(knowledgeBase)).thenReturn("user_7_kb_42");
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
        KnowledgeBaseEntity knowledgeBase = knowledgeBase(42L, "private-doc", "user_7_kb_42");
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(42L)))
            .thenReturn(List.of(knowledgeBase));
        when(scopeService.workspaceFor(knowledgeBase)).thenReturn("user_7_kb_42");
        when(documentService.refreshTrackedStatus(knowledgeBase)).thenReturn("COMPLETED");
        when(scopeService.allowedReferenceSources(List.of(knowledgeBase)))
            .thenReturn(List.of("knowledge-base/42/private.txt"));
        when(lightRagClient.query("question", "user_7_kb_42", List.of("knowledge-base/42/private.txt")))
            .thenReturn(new LightRagClient.LightRagQueryResult("isolated answer"));
        when(listService.getKnowledgeBaseNames(List.of(42L))).thenReturn(List.of("private-doc"));

        QueryResponse response = queryService.queryKnowledgeBase(request());

        assertEquals("isolated answer", response.answer());
        verify(lightRagClient).query("question", "user_7_kb_42", List.of("knowledge-base/42/private.txt"));
        verify(migrationService, never()).migrateAsync(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("multi-selection queries each knowledge-base workspace and combines answers")
    void queriesAndCombinesMultipleKnowledgeBaseWorkspaces() {
        KnowledgeBaseEntity first = knowledgeBase(42L, "first", "user_7_kb_42");
        KnowledgeBaseEntity second = knowledgeBase(43L, "second", "user_7_kb_43");
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(43L, 42L)))
            .thenReturn(List.of(first, second));
        when(scopeService.workspaceFor(first)).thenReturn("user_7_kb_42");
        when(scopeService.workspaceFor(second)).thenReturn("user_7_kb_43");
        when(documentService.refreshTrackedStatus(first)).thenReturn("COMPLETED");
        when(documentService.refreshTrackedStatus(second)).thenReturn("COMPLETED");
        when(scopeService.allowedReferenceSources(List.of(first))).thenReturn(List.of("knowledge-base/42/first.txt"));
        when(scopeService.allowedReferenceSources(List.of(second))).thenReturn(List.of("knowledge-base/43/second.txt"));
        when(lightRagClient.query("question", "user_7_kb_42", List.of("knowledge-base/42/first.txt")))
            .thenReturn(new LightRagClient.LightRagQueryResult("first answer"));
        when(lightRagClient.query("question", "user_7_kb_43", List.of("knowledge-base/43/second.txt")))
            .thenReturn(new LightRagClient.LightRagQueryResult("second answer"));
        when(listService.getKnowledgeBaseNames(List.of(43L, 42L))).thenReturn(List.of("second", "first"));

        QueryResponse response = queryService.queryKnowledgeBase(
            new QueryRequest(List.of(43L, 42L), "question", RagProvider.LIGHTRAG)
        );

        assertTrue(response.answer().startsWith("### second\n\nsecond answer"));
        assertTrue(response.answer().contains("### first\n\nfirst answer"));
        verify(lightRagClient).query("question", "user_7_kb_43", List.of("knowledge-base/43/second.txt"));
        verify(lightRagClient).query("question", "user_7_kb_42", List.of("knowledge-base/42/first.txt"));
    }

    @Test
    @DisplayName("multi-selection streams labelled answers from each isolated workspace")
    void streamsMultipleKnowledgeBaseWorkspaces() {
        KnowledgeBaseEntity first = knowledgeBase(42L, "first", "user_7_kb_42");
        KnowledgeBaseEntity second = knowledgeBase(43L, "second", "user_7_kb_43");
        when(currentUserService.requireCurrentUserId()).thenReturn(7L);
        when(knowledgeBaseRepository.findAllByOwner_IdAndIdIn(7L, List.of(42L, 43L)))
            .thenReturn(List.of(first, second));
        when(scopeService.workspaceFor(first)).thenReturn("user_7_kb_42");
        when(scopeService.workspaceFor(second)).thenReturn("user_7_kb_43");
        when(documentService.refreshTrackedStatus(first)).thenReturn("COMPLETED");
        when(documentService.refreshTrackedStatus(second)).thenReturn("COMPLETED");
        when(scopeService.allowedReferenceSources(List.of(first))).thenReturn(List.of("knowledge-base/42/first.txt"));
        when(scopeService.allowedReferenceSources(List.of(second))).thenReturn(List.of("knowledge-base/43/second.txt"));
        when(lightRagClient.queryStream("question", "user_7_kb_42", List.of("knowledge-base/42/first.txt")))
            .thenReturn(Flux.just("first answer"));
        when(lightRagClient.queryStream("question", "user_7_kb_43", List.of("knowledge-base/43/second.txt")))
            .thenReturn(Flux.just("second answer"));

        String answer = queryService.answerQuestionStream(List.of(42L, 43L), "question")
            .collectList()
            .map(chunks -> String.join("", chunks))
            .block();

        assertEquals("### first\n\nfirst answer\n\n### second\n\nsecond answer\n\n", answer);
    }

    private QueryRequest request() {
        return new QueryRequest(List.of(42L), "question", RagProvider.LIGHTRAG);
    }

    private KnowledgeBaseEntity knowledgeBase(String workspace) {
        return knowledgeBase(42L, "private-doc", workspace);
    }

    private KnowledgeBaseEntity knowledgeBase(Long id, String name, String workspace) {
        KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
        knowledgeBase.setId(id);
        knowledgeBase.setName(name);
        knowledgeBase.setOriginalFilename(name + ".txt");
        knowledgeBase.setLightRagStatus("COMPLETED");
        knowledgeBase.setLightRagWorkspace(workspace);
        return knowledgeBase;
    }
}
