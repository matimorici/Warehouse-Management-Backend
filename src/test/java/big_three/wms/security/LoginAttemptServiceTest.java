package big_three.wms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    @Test
    void isBlocked_unknownCuil_returnsFalse() {
        assertFalse(service.isBlocked("20-12345678-9"));
    }

    @Test
    void isBlocked_firstAttempt_returnsFalse() {
        service.recordFailure("20-12345678-9");
        assertFalse(service.isBlocked("20-12345678-9"));
    }

    @Test
    void isBlocked_fifthAttempt_returnsTrue() {
        for (int i = 0; i < 5; i++){
            service.recordFailure("20-12345678-9");
        }
        assertTrue(service.isBlocked("20-12345678-9"));
    }

    @Test
    void isBlocked_fifthAttempt_recordSuccess_returnsFalse() {
        for (int i = 0; i < 5; i++){
            service.recordFailure("20-12345678-9");
        }
        service.recordSuccess("20-12345678-9");
        assertFalse(service.isBlocked("20-12345678-9"));
    }

    @Test
    void isBlocked_stillBlockedJustBeforeLockExpires() {
        MutableClock clock = new MutableClock();
        LoginAttemptService timedService = new LoginAttemptService(clock);
        for (int i = 0; i < 5; i++) {
            timedService.recordFailure("20-12345678-9");
        }

        clock.advance(Duration.ofMinutes(14).plusSeconds(59));

        assertTrue(timedService.isBlocked("20-12345678-9"));
    }

    @Test
    void isBlocked_notBlockedOnceLockDurationPasses() {
        MutableClock clock = new MutableClock();
        LoginAttemptService timedService = new LoginAttemptService(clock);
        for (int i = 0; i < 5; i++) {
            timedService.recordFailure("20-12345678-9");
        }

        clock.advance(Duration.ofMinutes(15));

        assertFalse(timedService.isBlocked("20-12345678-9"));
    }

    @Test
    void recordFailure_afterLockExpires_startsANewCount() {
        MutableClock clock = new MutableClock();
        LoginAttemptService timedService = new LoginAttemptService(clock);
        for (int i = 0; i < 5; i++) {
            timedService.recordFailure("20-12345678-9");
        }

        clock.advance(Duration.ofMinutes(15));
        timedService.recordFailure("20-12345678-9");

        assertFalse(timedService.isBlocked("20-12345678-9"));
    }
}
