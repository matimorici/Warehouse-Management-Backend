package big_three.wms.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(LoginAttemptService.class)
class LoginAttemptServiceWiringTest {

    @Autowired
    private LoginAttemptService service;

    @Test
    void spring_canBuildTheServiceWithItsDefaultClock() {
        assertNotNull(service);
    }
}