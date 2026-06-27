package health.guardian.modules.auth;

import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.CurrentUserDTO;
import health.guardian.modules.auth.service.AuthService;
import health.guardian.modules.auth.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final AuthTokenService tokenService = new AuthTokenService();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicEndpoint(request)) {
            return true;
        }
        if (!request.getRequestURI().startsWith("/api/")) {
            return true;
        }

        Optional<String> token = tokenService.extractBearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (token.isEmpty()) {
            writeUnauthorized(response, "请先登录");
            return false;
        }

        Optional<CurrentUserDTO> currentUser = authService.authenticate(token.get());
        if (currentUser.isEmpty()) {
            writeUnauthorized(response, "登录状态已失效，请重新登录");
            return false;
        }

        CurrentUserContext.setCurrentUser(currentUser.get());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/register".equals(path)
            || "/api/auth/login".equals(path)
            || path.startsWith("/api/public-docs");
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
            "{\"code\":" + ErrorCode.UNAUTHORIZED.getCode()
                + ",\"message\":\"" + escapeJson(message) + "\",\"data\":null}"
        );
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
