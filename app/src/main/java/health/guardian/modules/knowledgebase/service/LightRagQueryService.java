package health.guardian.modules.knowledgebase.service;

import health.guardian.modules.knowledgebase.model.QueryRequest;
import health.guardian.modules.knowledgebase.model.QueryResponse;
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
    private final KnowledgeBaseListService listService;
    private final KnowledgeBaseCountService countService;

    public QueryResponse queryKnowledgeBase(QueryRequest request) {
        List<Long> knowledgeBaseIds = request.knowledgeBaseIds();
        countService.updateQuestionCounts(knowledgeBaseIds);

        LightRagClient.LightRagQueryResult result = lightRagClient.query(request.question());
        return buildResponse(knowledgeBaseIds, result.response());
    }

    public Flux<String> answerQuestionStream(List<Long> knowledgeBaseIds, String question) {
        countService.updateQuestionCounts(knowledgeBaseIds);
        return lightRagClient.queryStream(question)
            .onErrorResume(e -> {
                log.error("LightRAG streaming query failed: kbIds={}, error={}", knowledgeBaseIds, e.getMessage(), e);
                return Flux.just("【错误】LightRAG 查询失败：" + e.getMessage());
            });
    }

    private QueryResponse buildResponse(List<Long> knowledgeBaseIds, String answer) {
        List<String> kbNames = listService.getKnowledgeBaseNames(knowledgeBaseIds);
        String kbNamesStr = String.join("、", kbNames);
        Long primaryKbId = knowledgeBaseIds.getFirst();
        return new QueryResponse(answer, primaryKbId, kbNamesStr);
    }
}
