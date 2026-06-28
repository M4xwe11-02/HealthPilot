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

    public QueryResponse queryKnowledgeBase(QueryRequest request) {
        List<Long> knowledgeBaseIds = request.knowledgeBaseIds();
        String readinessMessage = validateReadiness(knowledgeBaseIds);
        if (readinessMessage != null) {
            return buildResponse(knowledgeBaseIds, readinessMessage);
        }
        countService.updateQuestionCounts(knowledgeBaseIds);

        LightRagClient.LightRagQueryResult result = lightRagClient.query(request.question());
        return buildResponse(knowledgeBaseIds, result.response());
    }

    public Flux<String> answerQuestionStream(List<Long> knowledgeBaseIds, String question) {
        String readinessMessage = validateReadiness(knowledgeBaseIds);
        if (readinessMessage != null) {
            return Flux.just(readinessMessage);
        }
        countService.updateQuestionCounts(knowledgeBaseIds);
        return lightRagClient.queryStream(question)
            .onErrorResume(e -> {
                log.error("LightRAG streaming query failed: kbIds={}, error={}", knowledgeBaseIds, e.getMessage(), e);
                return Flux.just("【错误】LightRAG 查询失败：" + e.getMessage());
            });
    }

    private String validateReadiness(List<Long> knowledgeBaseIds) {
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            return "请先选择知识库。";
        }

        Long ownerId = currentUserService.requireCurrentUserId();
        List<KnowledgeBaseEntity> entities = knowledgeBaseRepository.findAllByOwner_IdAndIdIn(
            ownerId,
            knowledgeBaseIds.stream().distinct().toList()
        );
        if (entities.size() != knowledgeBaseIds.stream().distinct().count()) {
            return "知识库不存在或无权访问。";
        }

        List<String> notReadyNames = entities.stream()
            .filter(kb -> !"COMPLETED".equals(lightRagDocumentService.refreshTrackedStatus(kb)))
            .map(KnowledgeBaseEntity::getName)
            .toList();

        if (notReadyNames.isEmpty()) {
            return null;
        }
        return "【提示】LightRAG 正在构建知识图谱，请稍后再试。未完成知识库：" + String.join("、", notReadyNames);
    }

    private QueryResponse buildResponse(List<Long> knowledgeBaseIds, String answer) {
        List<String> kbNames = listService.getKnowledgeBaseNames(knowledgeBaseIds);
        String kbNamesStr = String.join("、", kbNames);
        Long primaryKbId = knowledgeBaseIds == null || knowledgeBaseIds.isEmpty() ? null : knowledgeBaseIds.getFirst();
        return new QueryResponse(answer, primaryKbId, kbNamesStr);
    }
}
