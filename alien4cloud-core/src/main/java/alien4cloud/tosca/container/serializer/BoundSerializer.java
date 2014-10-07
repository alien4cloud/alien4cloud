package alien4cloud.tosca.container.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class BoundSerializer extends StdSerializer<Integer> {
    public static final String BOUND_SERIALIZER_AS_NUMBER = "alien4cloud.bound.serialzation.as.number";

    private static final String UNBOUNDED = "unbounded";

    protected BoundSerializer() {
        super(Integer.class);
    }

    @Override
    public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        if (value != null && value == Integer.MAX_VALUE && provider.getAttribute(BOUND_SERIALIZER_AS_NUMBER) == null) {
            jgen.writeString(UNBOUNDED);
        } else {
            jgen.writeNumber(value);
        }
    }
}
