package alien4cloud.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractTreeDiscriminatorPolymorphicDeserializer extends StdDeserializer<Map<String,Object>> {

    private final Map<String,Class<?>> registry = Maps.newHashMap();

    protected AbstractTreeDiscriminatorPolymorphicDeserializer() {
        super(Map.class);
    }

    @Override
    public Map<String,Object> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        ObjectMapper mapper = (ObjectMapper) parser.getCodec();

        ObjectNode root = mapper.readTree(parser);

        Map<String,Object> value = buildMap(mapper,root);

        return value;
    }

    private Map<String,Object> buildMap(ObjectMapper mapper,ObjectNode node) throws JsonProcessingException {
        Map<String,Object> result = Maps.newLinkedHashMap();

        Iterator<Map.Entry<String, JsonNode>> i = node.fields();
        while (i.hasNext()) {
            Map.Entry<String,JsonNode> e = i.next();
            result.put(e.getKey(),buildObject(mapper,e.getValue()));
        }

        return result;
    }

    private List<Object> buildList(ObjectMapper mapper, ArrayNode node) throws JsonProcessingException {
        List<Object> result = Lists.newArrayList();

        for (int i = 0 ; i < node.size() ; i++) {
            result.add(buildObject(mapper,node.get(i)));
        }

        return result;
    }

    private Object buildObject(ObjectMapper mapper,JsonNode node) throws JsonProcessingException {
        switch(node.getNodeType()) {
            case OBJECT:
                return resolve(mapper,(ObjectNode) node);
            case ARRAY:
                return buildList(mapper,(ArrayNode) node);
            case STRING:
                return ((TextNode) node).textValue();
            case BOOLEAN:
                return ((BooleanNode) node).booleanValue();
            case NUMBER:
                return ((NumericNode) node).asInt();
            default:
                return null;
        }
    }

    private Object resolve(ObjectMapper mapper,ObjectNode node) throws JsonProcessingException {
        Iterator<Map.Entry<String,JsonNode>> i = node.fields();
        while (i.hasNext()) {
            Map.Entry<String,JsonNode> e = i.next();

            if (registry.containsKey(e.getKey())) {
                return mapper.treeToValue(node,registry.get(e.getKey()));
            }
        }
        return buildMap(mapper,node);
    }

    protected final <T> void addToRegistry(String discriminator,Class<T> clazz) {
        registry.put(discriminator,clazz);
    }
}
