package alien4cloud.tosca.container.deserializer;

import java.io.IOException;

import alien4cloud.tosca.container.model.type.PropertyScope;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class PropertyScopeDeserializer extends StdDeserializer<PropertyScope> {
    private static final long serialVersionUID = -4312529767779669849L;

    public PropertyScopeDeserializer() {
        super(Object.class);
    }

    @Override
    public PropertyScope deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String value = jp.getText();
        if (value == null || value.equalsIgnoreCase("null")) {
            return null;
        }

        return PropertyScope.valueOf(value.toUpperCase());
    }
}