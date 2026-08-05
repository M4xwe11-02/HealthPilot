package health.guardian.modules.knowledgebase.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Builds LightRAG tenant scopes and document source identifiers.
 */
@Service
@RequiredArgsConstructor
public class LightRagScopeService {

    private static final String WORKSPACE_PREFIX = "user_";
    private static final String FILE_SOURCE_PREFIX = "knowledge-base/";

    private final CurrentUserService currentUserService;

    public String currentUserWorkspace() {
        return workspaceForOwnerId(currentUserService.requireCurrentUserId());
    }

    public String workspaceFor(KnowledgeBaseEntity knowledgeBase) {
        return workspaceForOwnerId(requireOwnerId(knowledgeBase));
    }

    public String fileSourceFor(KnowledgeBaseEntity knowledgeBase) {
        return fileSourcePrefixFor(knowledgeBase) + knowledgeBase.getOriginalFilename();
    }

    public List<String> allowedSourcePrefixes(List<KnowledgeBaseEntity> knowledgeBases) {
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return List.of();
        }
        return knowledgeBases.stream()
            .map(this::fileSourcePrefixFor)
            .distinct()
            .toList();
    }

    private String fileSourcePrefixFor(KnowledgeBaseEntity knowledgeBase) {
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "知识库缺少 LightRAG 来源标识");
        }
        return FILE_SOURCE_PREFIX + knowledgeBase.getId() + "/";
    }

    private String workspaceForOwnerId(Long ownerId) {
        if (ownerId == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "知识库缺少归属用户，无法隔离 LightRAG workspace");
        }
        return WORKSPACE_PREFIX + ownerId;
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
}
