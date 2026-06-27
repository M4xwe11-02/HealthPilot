package health.guardian.modules.knowledgebase.service;

import health.guardian.modules.knowledgebase.model.QueryRequest;
import health.guardian.modules.knowledgebase.model.QueryResponse;
import health.guardian.modules.knowledgebase.model.RagProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Routes RAG requests to the selected backend.
 */
@Service
@RequiredArgsConstructor
public class KnowledgeBaseQueryRouterService {

    private final KnowledgeBaseQueryService currentQueryService;
    private final LightRagQueryService lightRagQueryService;

    public QueryResponse queryKnowledgeBase(QueryRequest request) {
        return switch (RagProvider.normalize(request.ragProvider())) {
            case CURRENT -> currentQueryService.queryKnowledgeBase(request);
            case LIGHTRAG -> lightRagQueryService.queryKnowledgeBase(request);
        };
    }

    public Flux<String> answerQuestionStream(List<Long> knowledgeBaseIds, String question, RagProvider provider) {
        return switch (RagProvider.normalize(provider)) {
            case CURRENT -> currentQueryService.answerQuestionStream(knowledgeBaseIds, question);
            case LIGHTRAG -> lightRagQueryService.answerQuestionStream(knowledgeBaseIds, question);
        };
    }
}
