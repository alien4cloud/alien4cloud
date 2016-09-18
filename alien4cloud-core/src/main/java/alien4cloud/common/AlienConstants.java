package alien4cloud.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import alien4cloud.utils.AlienUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlienConstants {
    public static final String GLOBAL_WORKSPACE_ID = "a4cglobal";
    public static final String APP_WORKSPACE_PREFIX = "a4capp-";
    public static final String GROUP_ALL = "_A4C_ALL";
    public static final String OPERATION_NAME_SEPARATOR = AlienUtils.COLON_SEPARATOR;
    public static final String ATTRIBUTES_NAME_SEPARATOR = OPERATION_NAME_SEPARATOR;
    public static final String STORAGE_AZ_VOLUMEID_SEPARATOR = "/";
}
