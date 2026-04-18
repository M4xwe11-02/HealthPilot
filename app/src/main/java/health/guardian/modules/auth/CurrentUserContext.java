package health.guardian.modules.auth;

import health.guardian.common.exception.BusinessException;
import health.guardian.common.exception.ErrorCode;
import health.guardian.modules.auth.model.CurrentUserDTO;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUserDTO> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setCurrentUser(CurrentUserDTO user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUserDTO requireCurrentUser() {
        CurrentUserDTO user = CURRENT_USER.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
