package health.guardian.modules.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    String username,

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6到100个字符之间")
    String password,

    @Size(max = 100, message = "昵称不能超过100个字符")
    String displayName,

    @Email(message = "邮箱格式不正确")
    @Size(max = 254, message = "邮箱不能超过254个字符")
    String email,

    @Pattern(regexp = "^$|\\d{6}$", message = "验证码必须为6位数字")
    String code
) {
}
