package health.guardian.modules.knowledgebase.service;

import health.guardian.modules.knowledgebase.model.QueryRequest;
import health.guardian.modules.knowledgebase.model.QueryResponse;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import health.guardian.modules.auth.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Knowledge-base query service backed by an external LightRAG server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LightRagQueryService {

    private final LightRagClient lightRagClient;
    private final LightRagDocumentService lightRagDocumentService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final CurrentUserService currentUserService;
    private final KnowledgeBaseListService listService;
    private final KnowledgeBaseCountService countService;
    private final LightRagScopeService lightRagScopeService;
    private final LightRagMigrationService migrationService;

    public QueryResponse queryKnowledgeBase(QueryRequest request) {
        List<Long> knowledgeBaseIds = request.knowledgeBaseIds();
        ReadinessResult readiness = validateReadiness(knowledgeBaseIds);
        if (!readiness.ready()) {
            return buildResponse(knowledgeBaseIds, readiness.message());
        }
        countService.updateQuestionCounts(knowledgeBaseIds);

        LightRagClient.LightRagQueryResult result = lightRagClient.query(
            request.question(),
            readiness.workspace(),
            lightRagScopeService.allowedSourcePrefixes(readiness.knowledgeBases())
        );
        return buildResponse(knowledgeBaseIds, result.response());
    }

    public Flux<String> answerQuestionStream(List<Long> knowledgeBaseIds, String question) {
        ReadinessResult readiness = validateReadiness(knowledgeBaseIds);
        if (!readiness.ready()) {
            return Flux.just(readiness.message());
        }
        countService.updateQuestionCounts(knowledgeBaseIds);
        return lightRagClient.queryStream(
                question,
                readiness.workspace(),
                lightRagScopeService.allowedSourcePrefixes(readiness.knowledgeBases())
            )
            .onErrorResume(e -> {
                log.error("LightRAG streaming query failed: kbIds={}, error={}", knowledgeBaseIds, e.getMessage(), e);
                return Flux.just("【错误】LightRAG 查询失败：" + e.getMessage());
            });
    }

    private ReadinessResult validateReadiness(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return ReadinessResult.notReady("请先选择知识库。");
        }

        Long ownerId = currentUserService.requireCurrentUserId();
        List<Long> uniqueIds = knowledgeBaseIds.stream().distinct().toList();
        List<KnowledgeBaseEntity> entities = knowledgeBaseRepository.findAllByOwner_IdAndIdIn(
            ownerId,
            uniqueIds
        );
        if (entities.size() != uniqueIds.size()) {
            return ReadinessResult.notReady("知识库不存在或无权访问。");
        }

        String workspace = lightRagScopeService.currentUserWorkspace();
        List<KnowledgeBaseEntity> legacyWorkspaceEntities = entities.stream()
            .filter(entity -> !workspace.equals(entity.getLightRagWorkspace()))
            .toList();
        if (!legacyWorkspaceEntities.isEmpty()) {
            migrationService.migrateAsync(legacyWorkspaceEntities);
            List<String> names = legacyWorkspaceEntities.stream()
                .map(KnowledgeBaseEntity::getName)
                .toList();
            return ReadinessResult.notReady(
                "【提示】正在将旧知识库迁移到当前用户的独立 LightRAG 空间，请稍后再试：" + String.join("、", names)
            );
        }

        List<String> notReadyNames = entities.stream()
            .filter(kb -> !"COMPLETED".equals(lightRagDocumentService.refreshTrackedStatus(kb)))
            .map(KnowledgeBaseEntity::getName)
            .toList();

        if (notReadyNames.isEmpty()) {
            return ReadinessResult.ready(entities, workspace);
        }
        return ReadinessResult.notReady("【提示】LightRAG 正在构建知识图谱，请稍后再试。未完成知识库：" + String.join("、", notReadyNames));
    }

    private QueryResponse buildResponse(List<Long> knowledgeBaseIds, String answer) {
        List<String> kbNames = knowledgeBaseIds == null ? List.of() : listService.getKnowledgeBaseNames(knowledgeBaseIds);
        String kbNamesStr = String.join("、", kbNames);
        Long primaryKbId = knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() ? null : knowledgeBaseIds.getFirst();
        return new QueryResponse(answer, primaryKbId, kbNamesStr);
    }

    private record ReadinessResult(
        boolean ready,
        List<KnowledgeBaseEntity> knowledgeBases,
        String workspace,
        String message
    ) {
        private static ReadinessResult ready(List<KnowledgeBaseEntity> knowledgeBases, String workspace) {
            return new ReadinessResult(true, knowledgeBases, workspace, null);
        }

        private static ReadinessResult notReady(String message) {
            return new ReadinessResult(false, List.of(), null, message);
        }
    }
}
