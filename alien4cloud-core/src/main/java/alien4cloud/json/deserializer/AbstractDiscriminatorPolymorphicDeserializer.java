package alien4cloud.json.deserializer;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

/**
 * Manages polymorphism deserialization for Jackson through discriminator field (based on field exists).
 */
public class AbstractDiscriminatorPolymorphicDeserializer<T> extends StdDeserializer<T> {
    private Map<String, Class<? extends T>> registry = Maps.newHashMap();
    private Class<? extends T> defaultType = null;
    private Class<? extends T> valueStringClass = null;

    public AbstractDiscriminatorPolymorphicDeserializer(Class<T> clazz) {
        super(clazz);
    }

    protected void addToRegistry(String discriminator, Class<? extends T> clazz) {
        registry.put(discriminator, clazz);
    }

    protected void setDefault(Class<? extends T> clazz) {
        defaultType = clazz;
    }

    /**
     * Define the class to use to be used for parsing in case the value is a string and not an object.
     * 
     * @param valueStringClass
     */
    protected void setValueStringClass(Class<? extends T> valueStringClass) {
        this.valueStringClass = valueStringClass;
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        if (this.valueStringClass != null && JsonToken.VALUE_STRING.equals(jp.getCurrentToken())) {
            String parameter = jp.getValueAsString();
            // parse from string value
            try {
                Constructor constructor = this.valueStringClass.getConstructor(String.class);
                return (T) constructor.newInstance(parameter);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new JsonParseException("Failed to create instance of <" + this.valueStringClass.getName() + "> from constructor using string parameter <"
                        + parameter + ">", jp.getCurrentLocation(), e);
            }
        }
        ObjectNode root = (ObjectNode) mapper.readTree(jp);
        Class<? extends T> parameterClass = null;
        Iterator<Map.Entry<String, JsonNode>> elementsIterator = root.fields();
        while (elementsIterator.hasNext()) {
            Map.Entry<String, JsonNode> element = elementsIterator.next();
            String name = element.getKey();
            if (registry.containsKey(name)) {
                parameterClass = registry.get(name);
                break;
            }
        }
        if (parameterClass == null) {
            if (defaultType == null) {
                return null;
            } else {
                parameterClass = defaultType;
            }
        }
        return mapper.treeToValue(root, parameterClass);
    }
}