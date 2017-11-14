package alien4cloud.utils;

import com.rits.cloning.Cloner;

import lombok.SneakyThrows;

/**
 * Simple utility to clone java objects using serialization.
 */
public class CloneUtil {
    private static final Cloner CLONER = new Cloner();

    @SneakyThrows
    public static <T> T clone(T object) {
        return CLONER.deepClone(object);
    }
}