package health.guardian.modules.auth;

import health.guardian.common.result.Result;
import health.guardian.modules.auth.model.AuthLoginRequest;
import health.guardian.modules.auth.model.AuthRegisterRequest;
import health.guardian.modules.auth.model.AuthResponse;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/register")
    public Result<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/api/auth/me")
    public Result<CurrentUserDTO> me() {
        return Result.success(CurrentUserContext.requireCurrentUser());
    }

    @PostMapping("/api/auth/logout")
    public Result<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return Result.success(null);
    }
}
