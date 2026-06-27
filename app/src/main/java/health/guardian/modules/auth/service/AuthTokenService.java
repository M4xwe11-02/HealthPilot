package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AuthTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String BEARER_PREFIX = "Bearer ";

    private final SecureRandom secureRandom = new SecureRandom();

    public String issueToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Token 处理失败");
        }
    }

    public Optional<String> extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }
}
