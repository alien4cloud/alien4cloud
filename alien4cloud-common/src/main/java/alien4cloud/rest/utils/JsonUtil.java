package alien4cloud.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.rest.model.RestResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Simple utility for JSon processing.
 * 
 * @author luc boutier
 */
@Slf4j
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    static {
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

    private JsonUtil() {
    }

    /**
     * Parse a {@link RestResponse} by using the specified dataType as the expected data object's class.
     * 
     * @param responseAsString
     *            The {@link RestResponse} as a JSon String
     * @param dataType
     *            The type of the data object.
     * @return The parsed {@link RestResponse} object matching the given JSon.
     * @throws JsonParseException
     *             In case of a JSon parsing issue.
     * @throws JsonMappingException
     *             In case of a JSon parsing issue.
     * @throws IOException
     *             In case of an IO error.
     */
    public static <T> RestResponse<T> read(String responseAsString, Class<T> dataType) throws IOException {
        try {
            JavaType restResponseType = OBJECT_MAPPER.getTypeFactory().constructParametricType(RestResponse.class, dataType);
            return OBJECT_MAPPER.readValue(responseAsString, restResponseType);
        } catch (IOException e) {
            log.error("Error deserializing object :\n" + responseAsString, e);
            throw e;
        }
    }

    /**
     * Deserialize json text to object
     * 
     * @param objectText
     * @param objectClass
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T readObject(String objectText, Class<T> objectClass) throws IOException {
        try {
            return OBJECT_MAPPER.readValue(objectText, objectClass);
        } catch (IOException e) {
            log.error("Error deserializing object :\n" + objectText, e);
            throw e;
        }
    }

    /**
     * Deserialize json stream to object
     *
     * @param jsonStream
     * @param objectClass
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T readObject(InputStream jsonStream, Class<T> objectClass) throws IOException {
        try {
            return OBJECT_MAPPER.readValue(jsonStream, objectClass);
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * Deserialize json text to object
     * 
     * @param objectText
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static <T> T readObject(String objectText) throws IOException {
        TypeReference<T> typeRef = new TypeReference<T>() {
        };
        try {
            return OBJECT_MAPPER.readValue(objectText, typeRef);
        } catch (IOException e) {
            log.error("Error deserializing object :\n" + objectText, e);
            throw e;
        }
    }

    /**
     * Parse a {@link RestResponse} without being interested in parameterized type
     * 
     * @param responseAsString
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public static RestResponse<?> read(String responseAsString) throws IOException {
        try {
            return OBJECT_MAPPER.readValue(responseAsString, RestResponse.class);
        } catch (IOException e) {
            log.error("Error deserializing object :\n" + responseAsString, e);
            throw e;
        }
    }

    /**
     * Serialize the given object in a JSon String.
     * 
     * @param obj
     *            The object to serialize.
     * @return The JSon serialization of the given object.
     * @throws JsonProcessingException
     *             In case of a failure in serialization.
     */
    public static String toString(Object obj) throws JsonProcessingException {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object " + obj, e);
            throw e;
        }
    }

    /**
     * Deserialize the given json string to a map
     * 
     * @param json
     *            json text
     * @return map object
     * @throws IOException
     */
    public static Map<String, Object> toMap(String json) throws IOException {
        try {
            JavaType mapStringObjectType = OBJECT_MAPPER.getTypeFactory().constructParametricType(HashMap.class, String.class, Object.class);
            return OBJECT_MAPPER.readValue(json, mapStringObjectType);
        } catch (IOException e) {
            log.error("Error deserializing object to map \n" + json, e);
            throw e;
        }
    }

    /**
     * Deserialize the given json string to a map
     * 
     * @param json
     * @param keyTypeClass
     * @param valueTypeClass
     * @return
     * @throws IOException
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyTypeClass, Class<V> valueTypeClass) throws IOException {
        try {
            JavaType mapStringObjectType = OBJECT_MAPPER.getTypeFactory().constructParametricType(HashMap.class, keyTypeClass, valueTypeClass);
            return OBJECT_MAPPER.readValue(json, mapStringObjectType);
        } catch (IOException e) {
            log.error("Error deserializing object to map \n" + json, e);
            throw e;
        }
    }

    /**
     * transform the given object to a map
     * 
     * @param object
     * @param keyTypeClass
     * @param valueTypeClass
     * @return
     * @throws IOException
     */
    public static <K, V> Map<K, V> toMap(Object object, Class<K> keyTypeClass, Class<V> valueTypeClass) throws IOException {
        return toMap(toString(object), keyTypeClass, valueTypeClass);
    }

    public static <V> V[] toArray(String json, Class<V> valueTypeClass) throws IOException {
        try {
            JavaType arrayStringObjectType = OBJECT_MAPPER.getTypeFactory().constructArrayType(valueTypeClass);
            return OBJECT_MAPPER.readValue(json, arrayStringObjectType);
        } catch (IOException e) {
            log.error("Error deserializing object to array \n" + json, e);
            throw e;
        }
    }

    /**
     * Deserialize the given json string to a list
     * 
     * @param json
     *            json text
     * @return list object
     * @throws IOException
     */
    public static <T> List<T> toList(String json, Class<T> clazz) throws IOException {
        try {
            JavaType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            log.error("Error deserializing object to list \n" + json, e);
            throw e;
        }
    }

    public static <T> List<T> toList(String json, Class<T> elementClass, Class<?> elementGenericClass) throws IOException {
        try {
            JavaType elementType = OBJECT_MAPPER.getTypeFactory().constructParametricType(elementClass, elementGenericClass);
            JavaType listType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return OBJECT_MAPPER.readValue(json, listType);
        } catch (IOException e) {
            log.error("Error deserializing object to list \n" + json, e);
            throw e;
        }
    }
}
