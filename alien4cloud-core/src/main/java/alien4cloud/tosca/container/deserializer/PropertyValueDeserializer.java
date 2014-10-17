package alien4cloud.tosca.container.deserializer;

import java.io.IOException;

import alien4cloud.tosca.container.model.template.PropertyValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class PropertyValueDeserializer extends StdDeserializer<PropertyValue> {

    private static final long serialVersionUID = 7843687342621694374L;

    public PropertyValueDeserializer() {
        super(Object.class);
    }

    @Override
    public PropertyValue deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        return new PropertyValue(jp.getText());
    }
}