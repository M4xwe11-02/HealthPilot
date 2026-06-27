package health.guardian.modules.auth.service;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHash hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return new PasswordHash(
            Base64.getEncoder().encodeToString(salt),
            hashWithSalt(password, salt)
        );
    }

    public boolean verify(String password, String storedSalt, String storedHash) {
        try {
            byte[] salt = Base64.getDecoder().decode(storedSalt);
            String candidate = hashWithSalt(password, salt);
            return MessageDigest.isEqual(
                Base64.getDecoder().decode(candidate),
                Base64.getDecoder().decode(storedHash)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String hashWithSalt(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "密码处理失败");
        }
    }

    public record PasswordHash(String salt, String hash) {
    }
}
