package alien4cloud.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlienConstants {
    public static final String GLOBAL_WORKSPACE_ID = "ALIEN_GLOBAL_WORKSPACE";
    public static final String APP_WORKSPACE_PREFIX = "app";
    public static final String OPERATION_NAME_SEPARATOR = AlienUtils.COLON_SEPARATOR;

    public static final String NO_API_DOC_PROFILE = "noApiDoc";
    public static final String API_DOC_PROFILE_FILTER = "!" + NO_API_DOC_PROFILE;
    public static final int DEFAULT_ES_SEARCH_SIZE = 50;
    public static final int MAX_ES_SEARCH_SIZE = 1000;
    public static final String ALIEN_INTERNAL_TAG = "icon";
    public static final String DEFAULT_CAPABILITY_FIELD_NAME = "defaultCapabilities";
    public static final String GROUP_NAME_ALL_USERS = "ALL_USERS";
}
