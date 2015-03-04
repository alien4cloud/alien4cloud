package alien4cloud.it.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.RestMapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Simple utility for JSon processing.
 * 
 * This is a clone of alien4cloud.rest.utils.JsonUtil for test usage.
 * Since the JSON deserializer can not find annotations in test context !!!
 */
@Slf4j
@Deprecated
public final class JsonTestUtil {

    private static ObjectMapper getOneObjectMapper(boolean writeNullMapValues) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, writeNullMapValues);
        return mapper;
    }

    private static ObjectMapper createRestMapper() {
        ObjectMapper mapper = new RestMapper();
        // FIXME: find a better way to make the mapper manages annotations
        SimpleModule module = new SimpleModule();
        // register the deserializer
        module.addDeserializer(AbstractPropertyValue.class, new PropertyValueDeserializer());
        mapper.registerModule(module);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        return mapper;
    }
    
    private static ObjectMapper getOneObjectMapper() {
        return getOneObjectMapper(false);
    }

    private JsonTestUtil() {
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
        ObjectMapper mapper = createRestMapper();
        JavaType restResponseType = mapper.getTypeFactory().constructParametricType(RestResponse.class, dataType);
        return mapper.readValue(responseAsString, restResponseType);
    }

    /**
     * Parse a {@link RestResponse} without being interested in parameterized type
     *
     * @param responseAsString
     * @return
     * @throws IOException
     */
    public static RestResponse<?> read(String responseAsString) throws IOException {
        return createRestMapper().readValue(responseAsString, RestResponse.class);
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
        return getOneObjectMapper().readValue(objectText, objectClass);
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
        return getOneObjectMapper().readValue(jsonStream, objectClass);
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
        return getOneObjectMapper().readValue(objectText, typeRef);
    }

    /**
     * Serialize the given object in a JSon String.
     *
     * @param obj The object to serialize.
     * @return The JSon serialization of the given object.
     * @throws JsonProcessingException In case of a failure in serialization.
     */
    public static String toString(Object obj) throws JsonProcessingException {
        return getOneObjectMapper().writeValueAsString(obj);
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
        return getOneObjectMapper(true).writeValueAsString(obj);
    }

    /**
     * Deserialize the given json string to a map
     *
     * @param json json text
     * @return map object
     * @throws IOException
     */
    public static Map<String, Object> toMap(String json) throws IOException {
        ObjectMapper mapper = getOneObjectMapper();
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
        ObjectMapper mapper = getOneObjectMapper();
        JavaType mapStringObjectType = mapper.getTypeFactory().constructParametricType(HashMap.class, keyTypeClass, valueTypeClass);
        return mapper.readValue(json, mapStringObjectType);
    }

    public static <V> V[] toArray(String json, Class<V> valueTypeClass) throws IOException {
        ObjectMapper mapper = createRestMapper();
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
        ObjectMapper mapper = getOneObjectMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, clazz);
        return mapper.readValue(json, type);
    }

    public static <T> List<T> toList(String json, Class<T> elementClass, Class<?> elementGenericClass) throws IOException {
        ObjectMapper mapper = getOneObjectMapper();
        JavaType elementType = mapper.getTypeFactory().constructParametricType(elementClass, elementGenericClass);
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return mapper.readValue(json, listType);
    }
}