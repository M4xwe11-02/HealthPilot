package health.guardian.modules.auth.repository;

import health.guardian.modules.auth.model.AuthSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSessionEntity, Long> {
    Optional<AuthSessionEntity> findByTokenHash(String tokenHash);
}
