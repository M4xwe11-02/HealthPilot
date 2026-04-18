package health.guardian.modules.auth.service;

import health.guardian.modules.auth.CurrentUserContext;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.model.UserEntity;
import health.guardian.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserDTO requireCurrentUser() {
        return CurrentUserContext.requireCurrentUser();
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().id();
    }

    public UserEntity requireCurrentUserReference() {
        return userRepository.getReferenceById(requireCurrentUserId());
    }
}
