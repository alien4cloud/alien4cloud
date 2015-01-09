package alien4cloud.json.deserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    public AbstractDiscriminatorPolymorphicDeserializer(Class<T> clazz) {
        super(clazz);
    }

    protected void addToRegistry(String discriminator, Class<? extends T> clazz) {
        registry.put(discriminator, clazz);
    }

    @Override
    public T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
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
            return null;
        }
        return mapper.treeToValue(root, parameterClass);
    }
}