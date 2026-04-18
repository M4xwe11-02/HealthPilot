package health.guardian.common.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigPropertiesTest {

    @Test
    void defaultsAllowPdfHealthReportUploadsWhenConfigBindingIsMissing() {
        AppConfigProperties properties = new AppConfigProperties();

        assertTrue(properties.getAllowedTypes().contains("application/pdf"));
    }
}
