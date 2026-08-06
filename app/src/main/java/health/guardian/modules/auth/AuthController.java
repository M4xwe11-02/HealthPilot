package health.guardian.modules.auth;

import health.guardian.common.annotation.RateLimit;
import health.guardian.common.result.Result;
import health.guardian.modules.auth.model.AuthLoginRequest;
import health.guardian.modules.auth.model.AuthRegisterRequest;
import health.guardian.modules.auth.model.AuthResponse;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.model.EmailCodeRequest;
import health.guardian.modules.auth.model.EmailLoginRequest;
import health.guardian.modules.auth.service.AuthService;
import health.guardian.modules.auth.service.EmailVerificationService;
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
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/api/auth/register")
    public Result<AuthResponse> register(@Valid @RequestBody AuthRegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @PostMapping("/api/auth/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/api/auth/email/code")
    @RateLimit(
        dimensions = RateLimit.Dimension.IP,
        count = 10,
        interval = 1,
        timeUnit = RateLimit.TimeUnit.HOURS
    )
    public Result<Void> sendEmailCode(@Valid @RequestBody EmailCodeRequest request) {
        emailVerificationService.sendCode(request.email());
        return Result.success(null);
    }

    @PostMapping("/api/auth/email/login")
    @RateLimit(
        dimensions = RateLimit.Dimension.IP,
        count = 30,
        interval = 10,
        timeUnit = RateLimit.TimeUnit.MINUTES
    )
    public Result<AuthResponse> loginWithEmail(@Valid @RequestBody EmailLoginRequest request) {
        String email = emailVerificationService.verifyAndConsume(request.email(), request.code());
        return Result.success(authService.loginWithEmail(email));
    }

    @PostMapping("/api/auth/email/bind")
    @RateLimit(
        dimensions = RateLimit.Dimension.USER,
        count = 10,
        interval = 1,
        timeUnit = RateLimit.TimeUnit.HOURS
    )
    public Result<CurrentUserDTO> bindEmail(@Valid @RequestBody EmailLoginRequest request) {
        CurrentUserDTO currentUser = CurrentUserContext.requireCurrentUser();
        String email = emailVerificationService.verifyAndConsume(request.email(), request.code());
        return Result.success(authService.bindEmail(currentUser.id(), email));
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
