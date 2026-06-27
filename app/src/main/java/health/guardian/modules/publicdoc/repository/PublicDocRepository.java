package health.guardian.modules.publicdoc.repository;

import health.guardian.modules.publicdoc.model.PublicDocEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDocRepository extends JpaRepository<PublicDocEntity, Long> {

    List<PublicDocEntity> findAllByIsActiveTrueOrderByUploadedAtDesc();

    Optional<PublicDocEntity> findByIdAndIsActiveTrue(Long id);
}
