package alien4cloud.utils.jackson;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Maps;

/**
 * Custom de serializer that de-serialize map from an array of {@link MapEntry}.
 */
public class JSonMapEntryArrayDeSerializer extends StdDeserializer<Map<?, ?>> implements ContextualDeserializer {
    private static final long serialVersionUID = 1L;

    protected JSonMapEntryArrayDeSerializer() {
        super(Map.class);
    }

    private JavaType keyType;
    private JavaType valueType;
    private JsonDeserializer<?> fallback;

    public JSonMapEntryArrayDeSerializer(JavaType keyType, JavaType valueType, JsonDeserializer<?> fallback) {
        super(Map.class);
        this.keyType = keyType;
        this.valueType = valueType;
        this.fallback = fallback;
    }

    @Override
    public Map<?, ?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        if (JsonToken.START_OBJECT.equals(jp.currentToken())) {
            return (Map<?, ?>) fallback.deserialize(jp, ctxt);
        }
        // deserialize the map from array of map entries
        Map<Object, Object> map = Maps.newLinkedHashMap();
        JavaType mapEntryType = TypeFactory.defaultInstance().constructSimpleType(MapEntry.class, new JavaType[] { keyType, valueType });
        JavaType mapEntryArrayType = TypeFactory.defaultInstance().constructArrayType(mapEntryType);
        ObjectCodec codec = jp.getCodec();
        if (codec == null) {
            throw new IllegalStateException("No ObjectCodec defined for the parser, can not deserialize JSON into Java objects");
        }
        MapEntry<Object, Object>[] entries = codec.readValue(jp, mapEntryArrayType);
        for (MapEntry<Object, Object> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (ConditionalEnabledHelper.isEnabled(ctxt, property)) {
            BeanDescription beanDesc = ctxt.getConfig().introspect(property.getType());
            JsonDeserializer<?> deserializer = ctxt.getFactory().createMapDeserializer(ctxt, (MapType) property.getType(), beanDesc);
            if (deserializer instanceof ContextualDeserializer) {
                deserializer = ((ContextualDeserializer) deserializer).createContextual(ctxt, property);
            }
            return new JSonMapEntryArrayDeSerializer(property.getType().getKeyType(), property.getType().getContentType(), deserializer);
        }
        BeanDescription beanDesc = ctxt.getConfig().introspect(property.getType());
        JsonDeserializer<?> deserializer = ctxt.getFactory().createMapDeserializer(ctxt, (MapType) property.getType(), beanDesc);
        if (deserializer instanceof ContextualDeserializer) {
            return ((ContextualDeserializer) deserializer).createContextual(ctxt, property);
        }
        return deserializer;
    }
}
