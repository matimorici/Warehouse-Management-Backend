package big_three.wms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

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
}
