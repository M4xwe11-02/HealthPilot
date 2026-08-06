package health.guardian.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class EmailCodeStore {

    private static final String KEY_PREFIX = "auth:email-code:";

    private final RedissonClient redissonClient;
    private final PasswordHasher passwordHasher;

    public SendReservation reserveSend(
        String identity,
        Duration cooldown,
        Duration limitWindow,
        int maxSends
    ) {
        return withLock(identity, () -> {
            RBucket<String> cooldownBucket = stringBucket(cooldownKey(identity));
            if (cooldownBucket.isExists()) {
                return SendReservation.COOLDOWN;
            }

            RAtomicLong sendCounter = redissonClient.getAtomicLong(sendCountKey(identity));
            if (sendCounter.get() >= maxSends) {
                return SendReservation.LIMIT_REACHED;
            }

            long count = sendCounter.incrementAndGet();
            if (count == 1) {
                sendCounter.expire(limitWindow);
            }
            cooldownBucket.set("1", cooldown);
            return SendReservation.ACQUIRED;
        });
    }

    public void saveCode(
        String identity,
        PasswordHasher.PasswordHash passwordHash,
        Duration ttl
    ) {
        withLock(identity, () -> {
            stringBucket(codeKey(identity)).set(encode(passwordHash), ttl);
            redissonClient.getAtomicLong(attemptKey(identity)).delete();
            return null;
        });
    }

    public ConsumeResult consume(String identity, String candidateCode, int maxAttempts) {
        return withLock(identity, () -> {
            RBucket<String> codeBucket = stringBucket(codeKey(identity));
            String stored = codeBucket.get();
            PasswordHasher.PasswordHash passwordHash = decode(stored);
            if (passwordHash == null) {
                codeBucket.delete();
                redissonClient.getAtomicLong(attemptKey(identity)).delete();
                return ConsumeResult.MISSING;
            }

            if (passwordHasher.verify(candidateCode, passwordHash.salt(), passwordHash.hash())) {
                codeBucket.delete();
                redissonClient.getAtomicLong(attemptKey(identity)).delete();
                return ConsumeResult.SUCCESS;
            }

            RAtomicLong attempts = redissonClient.getAtomicLong(attemptKey(identity));
            long attemptCount = attempts.incrementAndGet();
            long remainingTtl = codeBucket.remainTimeToLive();
            if (attemptCount == 1 && remainingTtl > 0) {
                attempts.expire(Duration.ofMillis(remainingTtl));
            }
            if (attemptCount >= maxAttempts) {
                codeBucket.delete();
                attempts.delete();
                return ConsumeResult.TOO_MANY_ATTEMPTS;
            }
            return ConsumeResult.INVALID;
        });
    }

    public void removeCode(String identity) {
        withLock(identity, () -> {
            stringBucket(codeKey(identity)).delete();
            redissonClient.getAtomicLong(attemptKey(identity)).delete();
            return null;
        });
    }

    public void releaseSend(String identity) {
        withLock(identity, () -> {
            stringBucket(cooldownKey(identity)).delete();
            RAtomicLong sendCounter = redissonClient.getAtomicLong(sendCountKey(identity));
            if (sendCounter.isExists()) {
                long count = sendCounter.decrementAndGet();
                if (count <= 0) {
                    sendCounter.delete();
                }
            }
            return null;
        });
    }

    private <T> T withLock(String identity, Supplier<T> operation) {
        RLock lock = redissonClient.getLock(lockKey(identity));
        lock.lock();
        try {
            return operation.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private RBucket<String> stringBucket(String key) {
        return redissonClient.getBucket(key, StringCodec.INSTANCE);
    }

    private String encode(PasswordHasher.PasswordHash passwordHash) {
        return passwordHash.salt() + ":" + passwordHash.hash();
    }

    private PasswordHasher.PasswordHash decode(String stored) {
        if (stored == null) {
            return null;
        }
        String[] parts = stored.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        return new PasswordHasher.PasswordHash(parts[0], parts[1]);
    }

    private String codeKey(String identity) {
        return KEY_PREFIX + "value:" + identity;
    }

    private String cooldownKey(String identity) {
        return KEY_PREFIX + "cooldown:" + identity;
    }

    private String sendCountKey(String identity) {
        return KEY_PREFIX + "send-count:" + identity;
    }

    private String attemptKey(String identity) {
        return KEY_PREFIX + "attempts:" + identity;
    }

    private String lockKey(String identity) {
        return KEY_PREFIX + "lock:" + identity;
    }

    public enum SendReservation {
        ACQUIRED,
        COOLDOWN,
        LIMIT_REACHED
    }

    public enum ConsumeResult {
        SUCCESS,
        MISSING,
        INVALID,
        TOO_MANY_ATTEMPTS
    }
}
