package alien4cloud.common;

import alien4cloud.utils.AlienUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlienConstants {
    public static final String GLOBAL_WORKSPACE_ID = "ALIEN_GLOBAL_WORKSPACE";
    public static final String APP_WORKSPACE_PREFIX = "app";
    public static final String GROUP_ALL = "_A4C_ALL";
    public static final String OPERATION_NAME_SEPARATOR = AlienUtils.COLON_SEPARATOR;
    public static final String ATTRIBUTES_NAME_SEPARATOR = OPERATION_NAME_SEPARATOR;
    public static final String STORAGE_AZ_VOLUMEID_SEPARATOR = "/";

    public static final String NO_API_DOC_PROFILE = "noApiDoc";
    public static final String API_DOC_PROFILE_FILTER = "!" + NO_API_DOC_PROFILE;
}
