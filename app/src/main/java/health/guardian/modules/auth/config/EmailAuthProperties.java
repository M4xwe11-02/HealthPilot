package health.guardian.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.auth.email")
public class EmailAuthProperties {

    private boolean enabled;
    private String from = "";
    private String senderName = "Health Guard";
    private Duration codeTtl = Duration.ofMinutes(5);
    private Duration resendCooldown = Duration.ofSeconds(60);
    private Duration sendLimitWindow = Duration.ofHours(1);
    private int maxSendsPerWindow = 5;
    private int maxVerifyAttempts = 5;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }

    public Duration getSendLimitWindow() {
        return sendLimitWindow;
    }

    public void setSendLimitWindow(Duration sendLimitWindow) {
        this.sendLimitWindow = sendLimitWindow;
    }

    public int getMaxSendsPerWindow() {
        return maxSendsPerWindow;
    }

    public void setMaxSendsPerWindow(int maxSendsPerWindow) {
        this.maxSendsPerWindow = maxSendsPerWindow;
    }

    public int getMaxVerifyAttempts() {
        return maxVerifyAttempts;
    }

    public void setMaxVerifyAttempts(int maxVerifyAttempts) {
        this.maxVerifyAttempts = maxVerifyAttempts;
    }
}
