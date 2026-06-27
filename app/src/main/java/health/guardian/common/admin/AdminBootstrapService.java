package health.guardian.common.admin;

import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.auth.repository.UserRepository;
import health.guardian.modules.auth.service.PasswordHasher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @PostConstruct
    @Transactional
    public void ensureAdminExists() {
        if (userRepository.existsByUsername(ADMIN_USERNAME)) {
            return;
        }
        PasswordHasher.PasswordHash ph = passwordHasher.hash(ADMIN_PASSWORD);
        UserEntity admin = new UserEntity();
        admin.setUsername(ADMIN_USERNAME);
        admin.setDisplayName("管理员");
        admin.setPasswordSalt(ph.salt());
        admin.setPasswordHash(ph.hash());
        admin.setAdmin(true);
        userRepository.save(admin);
        log.info("管理员账号已初始化: username={}", ADMIN_USERNAME);
    }
}
