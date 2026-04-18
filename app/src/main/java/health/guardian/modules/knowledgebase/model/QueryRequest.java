package health.guardian.modules.knowledgebase.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Knowledge base query request.
 */
public record QueryRequest(
    @NotEmpty(message = "至少选择一个知识库")
    List<Long> knowledgeBaseIds,

    @NotBlank(message = "问题不能为空")
    String question,

    RagProvider ragProvider
) {
    public QueryRequest {
        ragProvider = RagProvider.normalize(ragProvider);
    }

    public QueryRequest(Long knowledgeBaseId, String question) {
        this(List.of(knowledgeBaseId), question, RagProvider.CURRENT);
    }
}
