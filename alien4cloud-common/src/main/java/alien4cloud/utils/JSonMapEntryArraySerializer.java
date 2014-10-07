package alien4cloud.utils;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom serializer that serialize map as an array of {@link MapEntry}.
 *
 * @author luc boutier
 */
public class JSonMapEntryArraySerializer extends StdSerializer<Map<?, ?>> {
    public static final String MAP_SERIALIZER_AS_ARRAY = "alien4cloud.map.serialzation.as.array";

    protected JSonMapEntryArraySerializer() {
        super(Map.class, false);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void serialize(Map<?, ?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (provider.getAttribute(MAP_SERIALIZER_AS_ARRAY) == null) {
            jgen.writeObject(value);
        } else {
            MapEntry[] entries = new MapEntry[value.size()];
            int i = 0;
            for (Entry<?, ?> entry : value.entrySet()) {
                entries[i] = new MapEntry(entry.getKey(), entry.getValue());
                i++;
            }
            jgen.writeObject(entries);
        }
    }
}
