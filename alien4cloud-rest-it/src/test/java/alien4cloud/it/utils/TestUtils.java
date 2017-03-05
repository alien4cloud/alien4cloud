package alien4cloud.it.utils;

import java.io.IOException;

import com.google.common.base.Joiner;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.rest.utils.JsonUtil;

/**
 */
public final class TestUtils {
    public static String getFullId(String name, String version) {
        return Joiner.on(":").join(name, version).toString();
    }

    public static String getNameFromId(String id) {
        return id.split(":")[0];
    }

    public static String getVersionFromId(String id) {
        return id.split(":")[1];
    }

    public static String nullable(String parameter) {
        return parameter == null || parameter.equals("null") ? null : parameter;
    }

    public static String nullAsString(String parameter) {
        return parameter == null ? "null" : parameter;
    }

    public static <T> void convert(GetMultipleDataResult untyped, T[] target, Class<T> clazz) throws IOException {
        for (int i = 0; i < untyped.getData().length; i++) {
            target[i] = JsonUtil.readObject(JsonUtil.toString(untyped.getData()[i]), clazz);
        }
    }
}