package big_three.wms.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CookieSecurityPropertiesTest {

    @Test
    void sessionCookie_isSecureAndHttpOnlyByDefault() throws IOException {
        Properties props = PropertiesLoaderUtils.loadAllProperties("application.properties");

        assertEquals("true", props.getProperty("server.servlet.session.cookie.secure"));
        assertEquals("true", props.getProperty("server.servlet.session.cookie.http-only"));
    }
}
