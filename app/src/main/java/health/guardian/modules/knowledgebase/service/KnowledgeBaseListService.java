package health.guardian.modules.knowledgebase.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.file.FileStorageService;
import health.guardian.infrastructure.mapper.KnowledgeBaseMapper;
import health.guardian.modules.auth.service.CurrentUserService;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseEntity;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseListItemDTO;
import health.guardian.modules.knowledgebase.model.KnowledgeBaseStatsDTO;
import health.guardian.modules.knowledgebase.model.RagChatMessageEntity.MessageType;
import health.guardian.modules.knowledgebase.model.VectorStatus;
import health.guardian.modules.knowledgebase.repository.KnowledgeBaseRepository;
import health.guardian.modules.knowledgebase.repository.RagChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseListService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagChatMessageRepository ragChatMessageRepository;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final FileStorageService fileStorageService;
    private final CurrentUserService currentUserService;

    public List<KnowledgeBaseListItemDTO> listKnowledgeBases(VectorStatus vectorStatus, String sortBy) {
        Long ownerId = currentUserService.requireCurrentUserId();
        List<KnowledgeBaseEntity> entities = vectorStatus != null
            ? knowledgeBaseRepository.findByOwner_IdAndVectorStatusOrderByUploadedAtDesc(ownerId, vectorStatus)
            : knowledgeBaseRepository.findAllByOwner_IdOrderByUploadedAtDesc(ownerId);

        if (sortBy != null && !sortBy.isBlank() && !sortBy.equalsIgnoreCase("time")) {
            entities = sortEntities(entities, sortBy);
        }

        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    public List<KnowledgeBaseListItemDTO> listKnowledgeBases() {
        return listKnowledgeBases(null, null);
    }

    public List<KnowledgeBaseListItemDTO> listKnowledgeBasesByStatus(VectorStatus vectorStatus) {
        return listKnowledgeBases(vectorStatus, null);
    }

    public Optional<KnowledgeBaseListItemDTO> getKnowledgeBase(Long id) {
        return knowledgeBaseRepository.findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId())
            .map(knowledgeBaseMapper::toListItemDTO);
    }

    public Optional<KnowledgeBaseEntity> getKnowledgeBaseEntity(Long id) {
        return knowledgeBaseRepository.findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId());
    }

    public List<String> getKnowledgeBaseNames(List<Long> ids) {
        Long ownerId = currentUserService.requireCurrentUserId();
        return ids.stream()
            .map(id -> knowledgeBaseRepository.findByIdAndOwner_Id(id, ownerId)
                .map(KnowledgeBaseEntity::getName)
                .orElse("未知知识库"))
            .toList();
    }

    public List<String> getAllCategories() {
        return knowledgeBaseRepository.findAllCategoriesForOwner(currentUserService.requireCurrentUserId());
    }

    public List<KnowledgeBaseListItemDTO> listByCategory(String category) {
        Long ownerId = currentUserService.requireCurrentUserId();
        List<KnowledgeBaseEntity> entities = category == null || category.isBlank()
            ? knowledgeBaseRepository.findByOwner_IdAndCategoryIsNullOrderByUploadedAtDesc(ownerId)
            : knowledgeBaseRepository.findByOwner_IdAndCategoryOrderByUploadedAtDesc(ownerId, category);
        return knowledgeBaseMapper.toListItemDTOList(entities);
    }

    @Transactional
    public void updateCategory(Long id, String category) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository
            .findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库不存在"));
        entity.setCategory(category != null && !category.isBlank() ? category : null);
        knowledgeBaseRepository.save(entity);
        log.info("更新知识库分类: id={}, category={}", id, category);
    }

    public List<KnowledgeBaseListItemDTO> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return listKnowledgeBases();
        }
        return knowledgeBaseMapper.toListItemDTOList(
            knowledgeBaseRepository.searchByOwnerAndKeyword(
                currentUserService.requireCurrentUserId(),
                keyword.trim()
            )
        );
    }

    public List<KnowledgeBaseListItemDTO> listSorted(String sortBy) {
        return listKnowledgeBases(null, sortBy);
    }

    private List<KnowledgeBaseEntity> sortEntities(List<KnowledgeBaseEntity> entities, String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "size" -> entities.stream()
                .sorted((a, b) -> Long.compare(nullToZero(b.getFileSize()), nullToZero(a.getFileSize())))
                .toList();
            case "access" -> entities.stream()
                .sorted((a, b) -> Integer.compare(nullToZero(b.getAccessCount()), nullToZero(a.getAccessCount())))
                .toList();
            case "question" -> entities.stream()
                .sorted((a, b) -> Integer.compare(nullToZero(b.getQuestionCount()), nullToZero(a.getQuestionCount())))
                .toList();
            default -> entities;
        };
    }

    public KnowledgeBaseStatsDTO getStatistics() {
        Long ownerId = currentUserService.requireCurrentUserId();
        return new KnowledgeBaseStatsDTO(
            knowledgeBaseRepository.countByOwner_Id(ownerId),
            ragChatMessageRepository.countBySession_Owner_IdAndType(ownerId, MessageType.USER),
            knowledgeBaseRepository.sumAccessCountForOwner(ownerId),
            knowledgeBaseRepository.countByOwner_IdAndVectorStatus(ownerId, VectorStatus.COMPLETED),
            knowledgeBaseRepository.countByOwner_IdAndVectorStatus(ownerId, VectorStatus.PROCESSING)
        );
    }

    public byte[] downloadFile(Long id) {
        KnowledgeBaseEntity entity = getEntityForDownload(id);
        String storageKey = entity.getStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessException(ErrorCode.STORAGE_DOWNLOAD_FAILED, "文件存储信息不存在");
        }

        log.info("下载知识库文件: id={}, filename={}", id, entity.getOriginalFilename());
        return fileStorageService.downloadFile(storageKey);
    }

    public KnowledgeBaseEntity getEntityForDownload(Long id) {
        return knowledgeBaseRepository.findByIdAndOwner_Id(id, currentUserService.requireCurrentUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND, "知识库不存在"));
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
