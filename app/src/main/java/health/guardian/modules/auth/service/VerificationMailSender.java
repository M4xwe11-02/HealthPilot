package health.guardian.modules.auth.service;

import java.time.Duration;

public interface VerificationMailSender {

    void sendCode(String email, String code, Duration validity);
}
