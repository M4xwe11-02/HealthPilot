package health.guardian.modules.auth.model;

public record CurrentUserDTO(
    Long id,
    String username,
    String displayName,
    boolean isAdmin
) {
    public static CurrentUserDTO from(UserEntity user) {
        return new CurrentUserDTO(user.getId(), user.getUsername(), user.getDisplayName(), user.isAdmin());
    }
}
