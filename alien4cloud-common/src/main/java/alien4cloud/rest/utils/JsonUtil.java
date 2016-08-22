package alien4cloud.rest.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import alien4cloud.rest.model.RestResponse;
import com.fasterxml.jackson.databind.type.ArrayType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.LRUMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Simple utility for JSon processing.
 */
@Slf4j
public final class JsonUtil {

    private static ObjectMapper getNewObjectMapper(boolean writeNullMapValues) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, writeNullMapValues);
        return mapper;
    }

    private static ObjectMapper createRestMapper() {
        ObjectMapper mapper = new RestMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        return mapper;
    }

    private static ObjectMapper getNewObjectMapper() {
        return getNewObjectMapper(false);
    }

    private JsonUtil() {
    }

    /**
     * Parse a {@link RestResponse} by using the specified dataType as the expected data object's class.
     *
     * @param responseAsString The {@link RestResponse} as a JSon String
     * @param dataType The type of the data object.
     * @return The parsed {@link RestResponse} object matching the given JSon.
     * @throws IOException In case of an IO error.
     */
    public static <T> RestResponse<T> read(String responseAsString, Class<T> dataType) throws IOException {
        return read(responseAsString, dataType, createRestMapper());
    }

    /**
     * Parse a {@link RestResponse} by using the specified dataType as the expected data object's class.
     *
     * @param responseAsString The {@link RestResponse} as a JSon String
     * @param dataType The type of the data object.
     * @param @param mapper the {@link ObjectMapper} to use
     * @return The parsed {@link RestResponse} object matching the given JSon.
     * @throws IOException In case of an IO error.
     */
    public static <T> RestResponse<T> read(String responseAsString, Class<T> dataType, ObjectMapper mapper) throws IOException {
        JavaType innerType = constructType(dataType, mapper.getTypeFactory());
        JavaType restResponseType = mapper.getTypeFactory().constructParametricType(RestResponse.class, innerType);
        return mapper.readValue(responseAsString, restResponseType);
    }

    private static <T> JavaType constructType(Class<T> dataType, TypeFactory typeFactory) {
        if (dataType == String.class || dataType == Boolean.TYPE || dataType == Integer.TYPE || dataType == Long.TYPE) {
            return typeFactory.constructSimpleType(dataType, null);
        }
        if (dataType.isArray()) {
            return typeFactory.constructArrayType(dataType.getComponentType());
        } else if (dataType.isEnum()) {
            return typeFactory.constructSimpleType(dataType, new JavaType[0]);
        } else if (Map.class.isAssignableFrom(dataType)) {
            return typeFactory.constructRawMapType((Class<? extends Map>) dataType);
        } else if (Collection.class.isAssignableFrom(dataType)) {
            return typeFactory.constructRawCollectionType((Class<? extends Collection>) dataType);
        }

        TypeVariable<Class<T>>[] types = dataType.getTypeParameters();
        JavaType[] javaTypes = new JavaType[types.length];
        for (int i = 0; i < javaTypes.length; i++) {
            javaTypes[i] = TypeFactory.unknownType();
        }

        return typeFactory.constructSimpleType(dataType, javaTypes);
    }

    /**
     * Parse a {@link RestResponse} without being interested in parameterized type
     *
     * @param responseAsString
     * @return
     * @throws IOException
     */
    public static RestResponse<?> read(String responseAsString) throws IOException {
        return read(responseAsString, createRestMapper());
    }

    /**
     * Parse a {@link RestResponse} without being interested in parameterized type
     *
     * @param responseAsString
     * @param mapper the {@link ObjectMapper} to use
     * @return
     * @throws IOException
     */
    public static RestResponse<?> read(String responseAsString, ObjectMapper mapper) throws IOException {
        return mapper.readValue(responseAsString, RestResponse.class);
    }

    /**
     * Deserialize json text to object
     *
     * @param objectText
     * @param objectClass
     * @return
     * @throws IOException
     */
    public static <T> T readObject(String objectText, Class<T> objectClass) throws IOException {
        return getNewObjectMapper().readValue(objectText, objectClass);
    }

    /**
     * Deserialize json stream to object
     *
     * @param jsonStream
     * @param objectClass
     * @return
     * @throws IOException
     */
    public static <T> T readObject(InputStream jsonStream, Class<T> objectClass) throws IOException {
        return getNewObjectMapper().readValue(jsonStream, objectClass);
    }

    /**
     * Deserialize json text to object
     *
     * @param objectText
     * @return
     * @throws IOException
     */
    public static <T> T readObject(String objectText) throws IOException {
        TypeReference<T> typeRef = new TypeReference<T>() {
        };
        return getNewObjectMapper().readValue(objectText, typeRef);
    }

    /**
     * Serialize the given object in a JSon String.
     *
     * @param obj The object to serialize.
     * @return The JSon serialization of the given object.
     * @throws JsonProcessingException In case of a failure in serialization.
     */
    public static String toString(Object obj) throws JsonProcessingException {
        return getNewObjectMapper().writeValueAsString(obj);
    }

    /**
     * Convert a map or list to an object
     * 
     * @param raw a map or a list
     * @param targetType the target class
     * @param <T> the target
     * @return
     */
    public static <T> T toObject(Object raw, Class<T> targetType) {
        return getNewObjectMapper().convertValue(raw, targetType);
    }

    /**
     * Serialize the given object in a JSon String (including null map entries).
     *
     * @param obj
     *            The object to serialize.
     * @return The JSon serialization of the given object.
     * @throws JsonProcessingException
     *             In case of a failure in serialization.
     */
    public static String toVerboseString(Object obj) throws JsonProcessingException {
        return getNewObjectMapper(true).writeValueAsString(obj);
    }

    /**
     * Deserialize the given json string to a map
     *
     * @param json json text
     * @return map object
     * @throws IOException
     */
    public static Map<String, Object> toMap(String json) throws IOException {
        ObjectMapper mapper = getNewObjectMapper();
        JavaType mapStringObjectType = mapper.getTypeFactory().constructParametricType(HashMap.class, String.class, Object.class);
        return mapper.readValue(json, mapStringObjectType);
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
        return toMap(json, keyTypeClass, valueTypeClass, getNewObjectMapper());
    }

    /**
     * Deserialize the given json string to a map
     *
     * @param json
     * @param keyTypeClass
     * @param valueTypeClass
     * @param mapper the {@link ObjectMapper} to use
     * @return
     * @throws IOException
     */
    public static <K, V> Map<K, V> toMap(String json, Class<K> keyTypeClass, Class<V> valueTypeClass, ObjectMapper mapper) throws IOException {
        JavaType mapStringObjectType = mapper.getTypeFactory().constructParametricType(HashMap.class, keyTypeClass, valueTypeClass);
        return mapper.readValue(json, mapStringObjectType);
    }

    public static <V> V[] toArray(String json, Class<V> valueTypeClass) throws IOException {
        return toArray(json, valueTypeClass, createRestMapper());
    }

    public static <V> V[] toArray(String json, Class<V> valueTypeClass, ObjectMapper mapper) throws IOException {
        JavaType arrayStringObjectType = mapper.getTypeFactory().constructArrayType(valueTypeClass);
        return mapper.readValue(json, arrayStringObjectType);
    }

    /**
     * Deserialize the given json string to a list
     *
     * @param json json text
     * @return list object
     * @throws IOException
     */
    public static <T> List<T> toList(String json, Class<T> clazz) throws IOException {
        return toList(json, clazz, getNewObjectMapper());
    }

    /**
     * Deserialize the given json string to a list
     *
     * @param json json text
     * @param mapper the {@link ObjectMapper} to use
     * @return list object
     * @throws IOException
     */
    public static <T> List<T> toList(String json, Class<T> clazz, ObjectMapper mapper) throws IOException {
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(json, type);
    }

    public static <T> List<T> toList(String json, Class<T> elementClass, Class<?> elementGenericClass) throws IOException {
        return toList(json, elementClass, elementGenericClass, getNewObjectMapper());
    }

    public static <T> List<T> toList(String json, Class<T> elementClass, Class<?> elementGenericClass, ObjectMapper mapper) throws IOException {
        JavaType elementType = mapper.getTypeFactory().constructParametricType(elementClass, elementGenericClass);
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return mapper.readValue(json, listType);
    }
}