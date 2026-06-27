package health.guardian.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用配置属性
 */
@Component
@ConfigurationProperties(prefix = "app.healthreport")
public class AppConfigProperties {

    private static final List<String> DEFAULT_ALLOWED_TYPES = List.of(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain"
    );
    
    private String uploadDir;
    private List<String> allowedTypes = DEFAULT_ALLOWED_TYPES;
    
    public String getUploadDir() {
        return uploadDir;
    }
    
    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
    
    public List<String> getAllowedTypes() {
        return allowedTypes;
    }
    
    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes == null || allowedTypes.isEmpty()
            ? DEFAULT_ALLOWED_TYPES
            : allowedTypes;
    }
}
