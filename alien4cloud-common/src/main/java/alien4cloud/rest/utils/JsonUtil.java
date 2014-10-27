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

    private static ObjectMapper getOneObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        return mapper;
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
            ObjectMapper mapper = getOneObjectMapper();
            JavaType restResponseType = mapper.getTypeFactory().constructParametricType(RestResponse.class, dataType);
            return mapper.readValue(responseAsString, restResponseType);
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
            return getOneObjectMapper().readValue(objectText, objectClass);
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
            return getOneObjectMapper().readValue(jsonStream, objectClass);
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
            return getOneObjectMapper().readValue(objectText, typeRef);
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
            return getOneObjectMapper().readValue(responseAsString, RestResponse.class);
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
            return getOneObjectMapper().writeValueAsString(obj);
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
            ObjectMapper mapper = getOneObjectMapper();
            JavaType mapStringObjectType = mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Object.class);
            return mapper.readValue(json, mapStringObjectType);
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
            ObjectMapper mapper = getOneObjectMapper();
            JavaType mapStringObjectType = mapper.getTypeFactory().constructParametricType(HashMap.class, keyTypeClass, valueTypeClass);
            return mapper.readValue(json, mapStringObjectType);
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
            ObjectMapper mapper = getOneObjectMapper();
            JavaType arrayStringObjectType = mapper.getTypeFactory().constructArrayType(valueTypeClass);
            return mapper.readValue(json, arrayStringObjectType);
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
            ObjectMapper mapper = getOneObjectMapper();
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return mapper.readValue(json, type);
        } catch (IOException e) {
            log.error("Error deserializing object to list \n" + json, e);
            throw e;
        }
    }

    public static <T> List<T> toList(String json, Class<T> elementClass, Class<?> elementGenericClass) throws IOException {
        try {
            ObjectMapper mapper = getOneObjectMapper();
            JavaType elementType = mapper.getTypeFactory().constructParametricType(elementClass, elementGenericClass);
            JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
            return mapper.readValue(json, listType);
        } catch (IOException e) {
            log.error("Error deserializing object to list \n" + json, e);
            throw e;
        }
    }
}
