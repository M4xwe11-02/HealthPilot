package health.guardian.modules.auth;

import health.guardian.common.result.Result;
import health.guardian.modules.auth.model.AuthLoginRequest;
import health.guardian.modules.auth.model.AuthRegisterRequest;
import health.guardian.modules.auth.model.AuthResponse;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.service.AuthService;
import health.guardian.modules.auth.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth HTTP boundary")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthController authController = new AuthController(authService, emailVerificationService);
        AuthInterceptor authInterceptor = new AuthInterceptor(authService);

        mockMvc = MockMvcBuilders
            .standaloneSetup(authController, new ProtectedController())
            .addInterceptors(authInterceptor)
            .build();
    }

    @Test
    @DisplayName("register is public and returns current user")
    void registerIsPublicAndReturnsCurrentUser() throws Exception {
        when(authService.register(any(AuthRegisterRequest.class)))
            .thenReturn(new AuthResponse("token-1", new CurrentUserDTO(1L, "alice", "Alice", false, null)));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"alice","password":"secret123","displayName":"Alice"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.token", is("token-1")))
            .andExpect(jsonPath("$.data.user.username", is("alice")));
    }

    @Test
    @DisplayName("email code endpoint is public and accepts a valid email")
    void emailCodeEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/email/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"demo@qq.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)));

        verify(emailVerificationService).sendCode("demo@qq.com");
    }

    @Test
    @DisplayName("email login verifies the code and returns the normal auth response")
    void emailLoginReturnsAuthResponse() throws Exception {
        when(emailVerificationService.verifyAndConsume("demo@qq.com", "123456"))
            .thenReturn("demo@qq.com");
        when(authService.loginWithEmail("demo@qq.com"))
            .thenReturn(new AuthResponse("token-email", new CurrentUserDTO(5L, "mail_demo", "demo", false, "demo@qq.com")));

        mockMvc.perform(post("/api/auth/email/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"demo@qq.com","code":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.token", is("token-email")));

        verify(authService).loginWithEmail("demo@qq.com");
    }

    @Test
    @DisplayName("logged-in password account can bind a verified email")
    void loggedInAccountCanBindEmail() throws Exception {
        CurrentUserDTO loggedIn = new CurrentUserDTO(2L, "alice", "Alice", false, null);
        CurrentUserDTO bound = new CurrentUserDTO(2L, "alice", "Alice", false, "demo@qq.com");
        when(authService.authenticate("token-password")).thenReturn(Optional.of(loggedIn));
        when(emailVerificationService.verifyAndConsume("demo@qq.com", "123456"))
            .thenReturn("demo@qq.com");
        when(authService.bindEmail(2L, "demo@qq.com")).thenReturn(bound);

        mockMvc.perform(post("/api/auth/email/bind")
                .header("Authorization", "Bearer token-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"demo@qq.com","code":"123456"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.id", is(2)))
            .andExpect(jsonPath("$.data.email", is("demo@qq.com")));

        verify(authService).bindEmail(2L, "demo@qq.com");
    }

    @Test
    @DisplayName("protected api rejects missing bearer token")
    void protectedApiRejectsMissingBearerToken() throws Exception {
        mockMvc.perform(get("/api/protected/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(401)));
    }

    @Test
    @DisplayName("protected api accepts valid bearer token and exposes current user")
    void protectedApiAcceptsValidBearerTokenAndExposesCurrentUser() throws Exception {
        when(authService.authenticate("token-1"))
            .thenReturn(Optional.of(new CurrentUserDTO(1L, "alice", "Alice", false, null)));

        mockMvc.perform(get("/api/protected/ping")
                .header("Authorization", "Bearer token-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.username", is("alice")))
            .andExpect(jsonPath("$.data.userId", is("1")));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/protected/ping")
        Result<Map<String, String>> ping(HttpServletRequest request) {
            CurrentUserDTO currentUser = CurrentUserContext.requireCurrentUser();
            return Result.success(Map.of(
                "username", currentUser.username(),
                "userId", request.getAttribute("userId").toString()
            ));
        }
    }
}
