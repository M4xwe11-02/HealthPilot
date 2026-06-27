package health.guardian.modules.auth.model;

import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
    @NotBlank(message = "用户名不能为空")
    String username,

    @NotBlank(message = "密码不能为空")
    String password
) {
}
