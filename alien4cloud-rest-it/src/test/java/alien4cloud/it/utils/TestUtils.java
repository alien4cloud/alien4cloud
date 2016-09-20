package alien4cloud.it.utils;

import com.google.common.base.Joiner;

/**
 */
public final class TestUtils {
    public static String getFullId(String name, String version) {
        return Joiner.on(":").join(name, version).toString();
    }

}
