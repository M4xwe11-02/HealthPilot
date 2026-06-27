package health.guardian.modules.publicdoc.service;

import health.guardian.common.model.AsyncTaskStatus;
import health.guardian.modules.publicdoc.model.PublicDocAnalysisEntity;
import health.guardian.modules.publicdoc.model.PublicDocEntity;
import health.guardian.modules.publicdoc.repository.PublicDocAnalysisRepository;
import health.guardian.modules.publicdoc.repository.PublicDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDocPersistenceService {

    private final PublicDocRepository docRepository;
    private final PublicDocAnalysisRepository analysisRepository;

    @Transactional
    public PublicDocEntity saveDoc(PublicDocEntity entity) {
        return docRepository.save(entity);
    }

    @Transactional
    public void updateStatus(Long id, AsyncTaskStatus status, String error) {
        docRepository.findById(id).ifPresent(doc -> {
            doc.setAnalyzeStatus(status);
            doc.setAnalyzeError(error);
            docRepository.save(doc);
            log.debug("公共文档状态已更新: id={}, status={}", id, status);
        });
    }

    @Transactional
    public PublicDocAnalysisEntity saveAnalysis(Long publicDocId, String summary,
                                                String keyPointsJson, String applicablePopulation,
                                                String mainRecommendationsJson) {
        PublicDocAnalysisEntity entity = new PublicDocAnalysisEntity();
        entity.setPublicDocId(publicDocId);
        entity.setSummary(summary);
        entity.setKeyPointsJson(keyPointsJson);
        entity.setApplicablePopulation(applicablePopulation);
        entity.setMainRecommendationsJson(mainRecommendationsJson);
        PublicDocAnalysisEntity saved = analysisRepository.save(entity);
        log.info("公共文档分析结果已保存: analysisId={}, publicDocId={}", saved.getId(), publicDocId);
        return saved;
    }

    public Optional<PublicDocAnalysisEntity> findLatestAnalysis(Long publicDocId) {
        return analysisRepository.findFirstByPublicDocIdOrderByAnalyzedAtDesc(publicDocId);
    }

    @Transactional
    public void softDelete(Long id) {
        docRepository.findById(id).ifPresent(doc -> {
            doc.setActive(false);
            docRepository.save(doc);
            log.info("公共文档已下架: id={}", id);
        });
    }
}
