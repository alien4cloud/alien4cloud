package alien4cloud.utils.jackson;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom serializer that serialize map as an array of {@link MapEntry}.
 */
public class JSonMapEntryArraySerializer extends StdSerializer<Map<?, ?>> implements ContextualSerializer {
    private boolean enabled;

    protected JSonMapEntryArraySerializer() {
        super(Map.class, false);
    }

    public JSonMapEntryArraySerializer(boolean enabled) {
        super(Map.class, false);
        this.enabled = enabled;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void serialize(Map<?, ?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (this.enabled) {
            MapEntry[] entries = new MapEntry[value.size()];
            int i = 0;
            for (Entry<?, ?> entry : value.entrySet()) {
                entries[i] = new MapEntry(entry.getKey(), entry.getValue());
                i++;
            }
            jgen.writeObject(entries);
        } else {
            jgen.writeObject(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        return new JSonMapEntryArraySerializer(ConditionalEnabledHelper.isEnabled(prov, property));
    }
}
