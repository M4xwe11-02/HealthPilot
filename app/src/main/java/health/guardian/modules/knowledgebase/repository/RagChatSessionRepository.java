package health.guardian.modules.knowledgebase.repository;

import health.guardian.modules.knowledgebase.model.RagChatSessionEntity;
import health.guardian.modules.knowledgebase.model.RagChatSessionEntity.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** 
 * RAG聊天会话Repository
 */
@Repository
public interface RagChatSessionRepository extends JpaRepository<RagChatSessionEntity, Long> {

    /**
     * 按更新时间倒序获取所有活跃会话
     */
    List<RagChatSessionEntity> findByStatusOrderByUpdatedAtDesc(SessionStatus status);

    List<RagChatSessionEntity> findByOwner_IdAndStatusOrderByUpdatedAtDesc(Long ownerId, SessionStatus status);

    /**
     * 获取所有会话（按更新时间倒序）
     */
    List<RagChatSessionEntity> findAllByOrderByUpdatedAtDesc();

    List<RagChatSessionEntity> findAllByOwner_IdOrderByUpdatedAtDesc(Long ownerId);

    /**
     * 获取所有会话（按置顶状态和更新时间排序：置顶的在前，然后按更新时间倒序）
     */
    @Query("SELECT s FROM RagChatSessionEntity s ORDER BY s.isPinned DESC, s.updatedAt DESC")
    List<RagChatSessionEntity> findAllOrderByPinnedAndUpdatedAtDesc();

    @Query("SELECT s FROM RagChatSessionEntity s WHERE s.owner.id = :ownerId ORDER BY s.isPinned DESC, s.updatedAt DESC")
    List<RagChatSessionEntity> findAllOrderByPinnedAndUpdatedAtDescForOwner(@Param("ownerId") Long ownerId);

    /**
     * 根据知识库ID查找相关会话
     */
    @Query("SELECT DISTINCT s FROM RagChatSessionEntity s JOIN s.knowledgeBases kb WHERE kb.id IN :kbIds ORDER BY s.updatedAt DESC")
    List<RagChatSessionEntity> findByKnowledgeBaseIds(@Param("kbIds") List<Long> knowledgeBaseIds);

    @Query("SELECT DISTINCT s FROM RagChatSessionEntity s JOIN s.knowledgeBases kb WHERE s.owner.id = :ownerId AND kb.id IN :kbIds ORDER BY s.updatedAt DESC")
    List<RagChatSessionEntity> findByKnowledgeBaseIdsForOwner(@Param("ownerId") Long ownerId, @Param("kbIds") List<Long> knowledgeBaseIds);

    /**
     * 获取会话详情（带消息列表和知识库）
     * 注意：使用 DISTINCT 避免笛卡尔积导致的重复数据
     */
    @Query("SELECT DISTINCT s FROM RagChatSessionEntity s LEFT JOIN FETCH s.knowledgeBases WHERE s.id = :id")
    Optional<RagChatSessionEntity> findByIdWithMessagesAndKnowledgeBases(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM RagChatSessionEntity s LEFT JOIN FETCH s.knowledgeBases WHERE s.id = :id AND s.owner.id = :ownerId")
    Optional<RagChatSessionEntity> findByIdWithMessagesAndKnowledgeBasesForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    /**
     * 获取会话（带知识库，不带消息）
     */
    @Query("SELECT s FROM RagChatSessionEntity s LEFT JOIN FETCH s.knowledgeBases WHERE s.id = :id")
    Optional<RagChatSessionEntity> findByIdWithKnowledgeBases(@Param("id") Long id);

    @Query("SELECT s FROM RagChatSessionEntity s LEFT JOIN FETCH s.knowledgeBases WHERE s.id = :id AND s.owner.id = :ownerId")
    Optional<RagChatSessionEntity> findByIdWithKnowledgeBasesForOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    Optional<RagChatSessionEntity> findByIdAndOwner_Id(Long id, Long ownerId);

    boolean existsByIdAndOwner_Id(Long id, Long ownerId);

    List<RagChatSessionEntity> findByOwnerIsNull();
}
