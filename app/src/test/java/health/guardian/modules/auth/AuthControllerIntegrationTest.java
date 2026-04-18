package health.guardian.modules.auth;

import health.guardian.common.result.Result;
import health.guardian.modules.auth.model.AuthLoginRequest;
import health.guardian.modules.auth.model.AuthRegisterRequest;
import health.guardian.modules.auth.model.AuthResponse;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.service.AuthService;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AuthController authController = new AuthController(authService);
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
            .thenReturn(new AuthResponse("token-1", new CurrentUserDTO(1L, "alice", "Alice", false)));

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
            .thenReturn(Optional.of(new CurrentUserDTO(1L, "alice", "Alice", false)));

        mockMvc.perform(get("/api/protected/ping")
                .header("Authorization", "Bearer token-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", is(200)))
            .andExpect(jsonPath("$.data.username", is("alice")));
    }

    @RestController
    static class ProtectedController {
        @GetMapping("/api/protected/ping")
        Result<Map<String, String>> ping() {
            CurrentUserDTO currentUser = CurrentUserContext.requireCurrentUser();
            return Result.success(Map.of("username", currentUser.username()));
        }
    }
}
