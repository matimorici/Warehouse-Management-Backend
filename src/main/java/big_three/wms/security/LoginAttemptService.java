package big_three.wms.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
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

    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String cuil) {
        Attempt attempt = attempts.get(cuil);
        return attempt != null && attempt.lockedUntil() != null && attempt.lockedUntil().isAfter(clock.instant());
    }

    public void recordFailure(String cuil) {
        attempts.compute(cuil, (clave, actual) -> {
            Instant now = clock.instant();
            int previos = actual == null ? 0 : actual.lockedUntil() != null && !actual.lockedUntil().isAfter(now) ? 0 : actual.failures();
            int nuevosFallos = previos + 1;
            Instant lockedUntil = nuevosFallos < MAX_ATTEMPTS ? null : now.plus(LOCK_DURATION);
            return new Attempt(nuevosFallos, lockedUntil);
        });
    }

    public void recordSuccess(String cuil) {
        attempts.remove(cuil);
    }
}