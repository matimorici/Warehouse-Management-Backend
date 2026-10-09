package big_three.wms.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private record Attempt(int failures, Instant lockedUntil) {}

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String cuil) {
        Attempt attempt = attempts.get(cuil);
        return attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(Instant.now());
    }

    public void recordFailure(String cuil) {
        attempts.compute(cuil, (clave, actual) -> {
            int previos = actual == null ? 0 : actual.lockedUntil() != null && actual.lockedUntil().isBefore(Instant.now()) ? 0 : actual.failures();
            int nuevosFallos = previos + 1;
            Instant lockedUntil = actual == null ? null : nuevosFallos < MAX_ATTEMPTS ? null : Instant.now().plus(LOCK_DURATION);
            return new Attempt(nuevosFallos, lockedUntil);
        });
    }

    public void recordSuccess(String cuil) {
        attempts.remove(cuil);
    }
}