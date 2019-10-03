package alien4cloud.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Closeables;

/**
 * Utility to help parsing YAML files.
 * 
 * @author luc boutier
 */
public final class YamlParserUtil {

    private YamlParserUtil() {
    }

    private static final ObjectMapper YAML_OBJECT_MAPPER = createYamlObjectMapper();

    private static final Yaml snakeYaml = new Yaml();

    /**
     * Creates YAML object mapper
     * 
     * @return YAML object mapper
     */
    public static ObjectMapper createYamlObjectMapper() {
        return newObjectMapper(new YAMLFactory());
    }

    /**
     * Creates an object mapper
     * 
     * @param factory the Jason factory
     * @return Object Mapper of the factory parameter
     */
    private static ObjectMapper newObjectMapper(JsonFactory factory) {
        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        return mapper;
    }

    /**
     * Parses a file to get a clazz parameter class
     * 
     * @param filePath The path of the file to load an parse.
     * @param clazz The return instance class
     * @return An instance of T.
     * @throws IOException In case jackson fails to read the json input stream to create an instance of T.
     */
    public static <T> T parseFromUTF8File(String filePath, Class<T> clazz) throws IOException {
        Path path = Paths.get(filePath);
        return parseFromUTF8File(path, clazz);
    }

    /**
     * Load the file from the given path and parse it's content into an instance of T.
     * 
     * @param filePath The path of the file to load an parse.
     * @param clazz The return instance class
     * @return An instance of T.
     * @throws IOException In case jackson fails to read the json input stream to create an instance of T.
     */
    public static <T> T parseFromUTF8File(Path filePath, Class<T> clazz) throws IOException {
        InputStream input = Files.newInputStream(filePath);
        try {
            return parse(input, clazz);
        } finally {
            Closeables.close(input, true);
        }
    }

    /**
     * Parses a string to build an instance of T
     * 
     * @param strToParse An input stream that contains a json to parse.
     * @param clazz The class in which to deserialize the json.
     * @return an instance of T.
     * @throws IOException In case jackson fails to read the json input stream to create an instance of T.
     */
    public static <T> T parse(InputStream strToParse, Class<T> clazz) throws IOException {
        return YAML_OBJECT_MAPPER.readValue(strToParse, clazz);
    }

    /**
     * Parse text to build an instance of T.
     * 
     * @param json JSon string to parse.
     * @param clazz Class of T.
     * @return An instance of T from the provided json string.
     * @throws IOException In case jackson fails to read the json string to create an instance of T.
     */
    public static <T> T parse(String json, Class<T> clazz) throws IOException {
        return YAML_OBJECT_MAPPER.readValue(json, clazz);
    }

    /**
     * Write object to file.
     * 
     * @param object Object to write.
     * @param filePath Path of the file to write to.
     * @throws IOException In case we fail to write the object content to the given file path.
     */
    public static void write(Object object, String filePath) throws IOException {
        write(object, Paths.get(filePath));
    }

    /**
     * Write object to file
     * 
     * @param object object to write
     * @param path path of the file to write to
     * @throws IOException In case we fail to write the object content to the given file path.
     */
    public static void write(Object object, Path path) throws IOException {
        YAML_OBJECT_MAPPER.writeValue(path.toFile(), object);
    }

    /**
     * Serialize a YAML object to an output stream.
     * 
     * @param object object to write.
     * @param stream The stream in which to write the serialized object.
     * @throws IOException In case we fail to write the object content to the given file path.
     */
    public static void write(Object object, OutputStream stream) throws IOException {
        YAML_OBJECT_MAPPER.writeValue(stream, object);
    }

    /**
     * Serialize an object to YAML string using UTF-8 charset.
     * 
     * @param object The object to serialize.
     * @return A YAML string representation of the deserialized object.
     * @throws IOException In case we fail to write the object content to the given file path.
     */
    public static String toYaml(Object object) throws IOException {
        return YAML_OBJECT_MAPPER.writeValueAsString(object);
    }

    public static String dump(Object object) {
        return object == null ? null : (object instanceof Map ? dumpAsMap(object) : snakeYaml.dump(object));
    }

    public static String dumpAsMap(Object object) {
        return snakeYaml.dumpAsMap(object);
    }

    public static Object load(String yamlString) {
        return snakeYaml.load(yamlString);
    }
}