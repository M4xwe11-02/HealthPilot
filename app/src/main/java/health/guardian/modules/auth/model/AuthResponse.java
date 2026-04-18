package health.guardian.modules.auth.model;

public record AuthResponse(
    String token,
    CurrentUserDTO user
) {
}
