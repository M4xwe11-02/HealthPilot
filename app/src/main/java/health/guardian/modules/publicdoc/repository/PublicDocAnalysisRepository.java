package health.guardian.modules.publicdoc.repository;

import health.guardian.modules.publicdoc.model.PublicDocAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PublicDocAnalysisRepository extends JpaRepository<PublicDocAnalysisEntity, Long> {

    Optional<PublicDocAnalysisEntity> findFirstByPublicDocIdOrderByAnalyzedAtDesc(Long publicDocId);
}
