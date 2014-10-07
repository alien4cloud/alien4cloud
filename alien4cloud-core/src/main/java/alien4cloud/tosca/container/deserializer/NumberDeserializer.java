package alien4cloud.tosca.container.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class NumberDeserializer extends StdDeserializer<Number> {

    private static final long serialVersionUID = 2445951062310632497L;

    protected NumberDeserializer() {
        super(Number.class);
    }

    @Override
    public Number deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        Number number = jp.getNumberValue();
        if (number == null) {
            throw JsonMappingException.from(jp, "Reference value for the constraint is null");
        } else {
            return number;
        }
    }
}
