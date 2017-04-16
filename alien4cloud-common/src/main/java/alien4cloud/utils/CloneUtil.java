package alien4cloud.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

/**
 * Simple utility to clone java objects using serialization.
 */
public class CloneUtil {
    @SneakyThrows
    public static <T> T clone(T object) {
        ObjectMapper mapper = new ObjectMapper();
        final byte[] bytes = mapper.writeValueAsBytes(object);
        return mapper.readValue(bytes, (Class<T>) object.getClass());
    }
}