package alien4cloud.tosca.container.serializer;

import java.io.IOException;

import alien4cloud.tosca.container.model.template.PropertyValue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class PropertyValueSerializer extends StdSerializer<PropertyValue> {

    protected PropertyValueSerializer() {
        super(PropertyValue.class);
    }

    @Override
    public void serialize(PropertyValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
        jgen.writeString(value.getValue());
    }
}