package health.guardian.modules.publicdoc.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.infrastructure.redis.PublicDocCache;
import health.guardian.modules.publicdoc.model.PublicDocAnalysisEntity;
import health.guardian.modules.publicdoc.model.PublicDocDetailDTO;
import health.guardian.modules.publicdoc.model.PublicDocEntity;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocDetailService {

    private static final int TEXT_PREVIEW_LENGTH = 500;

    private final PublicDocRepository docRepository;
    private final PublicDocPersistenceService persistenceService;
    private final PublicDocCache cache;
    private final ObjectMapper objectMapper;

    public PublicDocDetailDTO getDetail(Long id) {
        return cache.getDetail(id).orElseGet(() -> {
            log.debug("公共文档详情缓存未命中，查询数据库: id={}", id);
            PublicDocEntity doc = docRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLIC_DOC_NOT_FOUND));

            PublicDocDetailDTO.Analysis analysis = buildAnalysis(id);
            String textPreview = buildTextPreview(doc.getDocText());

            PublicDocDetailDTO detail = new PublicDocDetailDTO(
                doc.getId(),
                doc.getTitle(),
                doc.getCategory(),
                doc.getSource(),
                doc.getDescription(),
                doc.getFileSize(),
                doc.getUploadedAt(),
                doc.getAnalyzeStatus(),
                doc.getDownloadCount(),
                textPreview,
                analysis
            );
            cache.saveDetail(id, detail);
            return detail;
        });
    }

    private PublicDocDetailDTO.Analysis buildAnalysis(Long publicDocId) {
        Optional<PublicDocAnalysisEntity> opt = persistenceService.findLatestAnalysis(publicDocId);
        if (opt.isEmpty()) {
            return null;
        }
        PublicDocAnalysisEntity entity = opt.get();
        return new PublicDocDetailDTO.Analysis(
            entity.getSummary(),
            parseJsonList(entity.getKeyPointsJson()),
            entity.getApplicablePopulation(),
            parseJsonList(entity.getMainRecommendationsJson())
        );
    }

    private String buildTextPreview(String docText) {
        if (docText == null || docText.isBlank()) return "";
        return docText.length() <= TEXT_PREVIEW_LENGTH
            ? docText
            : docText.substring(0, TEXT_PREVIEW_LENGTH) + "...";
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("反序列化JSON列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
