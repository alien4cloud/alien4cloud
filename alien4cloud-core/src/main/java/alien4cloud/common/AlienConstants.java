package alien4cloud.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import alien4cloud.utils.AlienUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AlienConstants {

    public static final String OPERATION_NAME_SEPARATOR = AlienUtils.COLON_SEPARATOR;
    public static final String STORAGE_AZ_VOLUMEID_SEPARATOR = "/";
}
