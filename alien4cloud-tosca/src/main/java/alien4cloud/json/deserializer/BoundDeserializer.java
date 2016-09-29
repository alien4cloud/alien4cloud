package alien4cloud.json.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;

/**
 * Utility class to deserialize the unbounded string as a int value for {@link RequirementDefinition} and
 * {@link CapabilityDefinition}.
 * 
 * @author luc boutier
 */
public class BoundDeserializer extends StdDeserializer<Integer> {

    private static final long serialVersionUID = 4915812266526752966L;

    protected BoundDeserializer() {
        super(Integer.class);
    }

    private static final String UNBOUNDED = "unbounded";

    @Override
    public Integer deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String value = jp.getValueAsString();
        if (UNBOUNDED.equals(value)) {
            return Integer.MAX_VALUE;
        }
        return jp.getIntValue();
    }
}