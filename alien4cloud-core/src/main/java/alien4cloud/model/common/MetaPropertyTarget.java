package alien4cloud.model.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Possible targets of a meta-property
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MetaPropertyTarget {
    public static final String APPLICATION = "application";
    public static final String LOCATION = "location";
}
