package health.guardian.modules.knowledgebase.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds LightRAG tenant scopes and document source identifiers.
 */
@Service
public class LightRagScopeService {

    private static final String FILE_SOURCE_PREFIX = "knowledge-base/";

    public String workspaceFor(KnowledgeBaseEntity knowledgeBase) {
        return "user_" + requireOwnerId(knowledgeBase) + "_kb_" + requireKnowledgeBaseId(knowledgeBase);
    }

    public String fileSourceFor(KnowledgeBaseEntity knowledgeBase) {
        return fileSourcePrefixFor(knowledgeBase) + knowledgeBase.getOriginalFilename();
    }

    public List<String> allowedReferenceSources(List<KnowledgeBaseEntity> knowledgeBases) {
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return List.of();
        }
        return knowledgeBases.stream()
            .map(this::fileSourceFor)
            .distinct()
            .toList();
    }

    private String fileSourcePrefixFor(KnowledgeBaseEntity knowledgeBase) {
        return FILE_SOURCE_PREFIX + requireKnowledgeBaseId(knowledgeBase) + "/";
    }

    private Long requireOwnerId(KnowledgeBaseEntity knowledgeBase) {
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "知识库不存在，无法生成 LightRAG workspace");
        }
        UserEntity owner = knowledgeBase.getOwner();
        if (owner == null || owner.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "知识库缺少归属用户，无法隔离 LightRAG workspace");
        }
        return owner.getId();
    }

    private Long requireKnowledgeBaseId(KnowledgeBaseEntity knowledgeBase) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "知识库缺少 LightRAG 来源标识");
        }
        return knowledgeBase.getId();
    }
}
