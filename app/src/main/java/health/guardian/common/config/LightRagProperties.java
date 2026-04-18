package health.guardian.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * LightRAG HTTP service configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.lightrag")
public class LightRagProperties {

    private boolean enabled = false;
    private boolean ingestOnUpload = true;
    private String baseUrl = "http://localhost:9621";
    private String apiKey = "";
    private String defaultMode = "mix";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration requestTimeout = Duration.ofMinutes(3);
}
