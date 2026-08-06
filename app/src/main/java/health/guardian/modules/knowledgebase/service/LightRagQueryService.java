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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        List<KnowledgeBaseAnswer> answers = readiness.knowledgeBases().stream()
            .map(knowledgeBase -> queryOneKnowledgeBase(knowledgeBase, request.question()))
            .toList();
        return buildResponse(knowledgeBaseIds, combineAnswers(answers));
    }

    public Flux<String> answerQuestionStream(List<Long> knowledgeBaseIds, String question) {
        ReadinessResult readiness = validateReadiness(knowledgeBaseIds);
        if (!readiness.ready()) {
            return Flux.just(readiness.message());
        }
        countService.updateQuestionCounts(knowledgeBaseIds);
        if (readiness.knowledgeBases().size() == 1) {
            return queryOneKnowledgeBaseStream(readiness.knowledgeBases().getFirst(), question);
        }

        return Flux.fromIterable(readiness.knowledgeBases())
            .concatMap(knowledgeBase -> Flux.concat(
                Flux.just("### " + knowledgeBase.getName() + "\n\n"),
                queryOneKnowledgeBaseStream(knowledgeBase, question),
                Flux.just("\n\n")
            ));
    }

    private ReadinessResult validateReadiness(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return ReadinessResult.notReady("请先选择知识库。");
        }

        Long ownerId = currentUserService.requireCurrentUserId();
        List<Long> uniqueIds = knowledgeBaseIds.stream().distinct().toList();
        List<KnowledgeBaseEntity> foundEntities = knowledgeBaseRepository.findAllByOwner_IdAndIdIn(
            ownerId,
            uniqueIds
        );
        if (foundEntities.size() != uniqueIds.size()) {
            return ReadinessResult.notReady("知识库不存在或无权访问。");
        }
        Map<Long, KnowledgeBaseEntity> entitiesById = foundEntities.stream().collect(Collectors.toMap(
            KnowledgeBaseEntity::getId,
            entity -> entity,
            (left, right) -> left,
            LinkedHashMap::new
        ));
        List<KnowledgeBaseEntity> entities = uniqueIds.stream().map(entitiesById::get).toList();

        List<KnowledgeBaseEntity> legacyWorkspaceEntities = entities.stream()
            .filter(entity -> !lightRagScopeService.workspaceFor(entity).equals(entity.getLightRagWorkspace()))
            .toList();
        if (!legacyWorkspaceEntities.isEmpty()) {
            migrationService.migrateAsync(legacyWorkspaceEntities);
            List<String> names = legacyWorkspaceEntities.stream()
                .map(KnowledgeBaseEntity::getName)
                .toList();
            return ReadinessResult.notReady(
                "【提示】正在将旧知识库迁移到独立 LightRAG 图谱，请稍后再试：" + String.join("、", names)
            );
        }

        List<String> notReadyNames = entities.stream()
            .filter(kb -> !"COMPLETED".equals(lightRagDocumentService.refreshTrackedStatus(kb)))
            .map(KnowledgeBaseEntity::getName)
            .toList();

        if (notReadyNames.isEmpty()) {
            return ReadinessResult.ready(entities);
        }
        return ReadinessResult.notReady("【提示】LightRAG 正在构建知识图谱，请稍后再试。未完成知识库：" + String.join("、", notReadyNames));
    }

    private KnowledgeBaseAnswer queryOneKnowledgeBase(KnowledgeBaseEntity knowledgeBase, String question) {
        LightRagClient.LightRagQueryResult result = lightRagClient.query(
            question,
            lightRagScopeService.workspaceFor(knowledgeBase),
            lightRagScopeService.allowedReferenceSources(List.of(knowledgeBase))
        );
        return new KnowledgeBaseAnswer(knowledgeBase.getName(), result.response());
    }

    private Flux<String> queryOneKnowledgeBaseStream(KnowledgeBaseEntity knowledgeBase, String question) {
        return lightRagClient.queryStream(
                question,
                lightRagScopeService.workspaceFor(knowledgeBase),
                lightRagScopeService.allowedReferenceSources(List.of(knowledgeBase))
            )
            .onErrorResume(error -> {
                log.error(
                    "LightRAG streaming query failed: kbId={}, workspace={}, error={}",
                    knowledgeBase.getId(),
                    lightRagScopeService.workspaceFor(knowledgeBase),
                    error.getMessage(),
                    error
                );
                return Flux.just("【错误】LightRAG 查询失败：" + error.getMessage());
            });
    }

    private String combineAnswers(List<KnowledgeBaseAnswer> answers) {
        if (answers.size() == 1) {
            return answers.getFirst().answer();
        }
        return answers.stream()
            .map(answer -> "### " + answer.knowledgeBaseName() + "\n\n" + answer.answer())
            .collect(Collectors.joining("\n\n"));
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
        String message
    ) {
        private static ReadinessResult ready(List<KnowledgeBaseEntity> knowledgeBases) {
            return new ReadinessResult(true, knowledgeBases, null);
        }

        private static ReadinessResult notReady(String message) {
            return new ReadinessResult(false, List.of(), message);
        }
    }

    private record KnowledgeBaseAnswer(String knowledgeBaseName, String answer) {
    }
}
