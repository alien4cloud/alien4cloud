package alien4cloud.tosca.container.deserializer;

import java.io.IOException;

import alien4cloud.tosca.container.model.type.PropertyScope;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class PropertyScopeSerializer extends StdSerializer<PropertyScope> {
    protected PropertyScopeSerializer() {
        super(PropertyScope.class);
    }

    @Override
    public void serialize(PropertyScope value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {
        jgen.writeString(value.toString().toLowerCase());
    }
}