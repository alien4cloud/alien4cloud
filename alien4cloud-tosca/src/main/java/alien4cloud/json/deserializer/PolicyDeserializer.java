package alien4cloud.json.deserializer;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.GenericPolicy;
import org.alien4cloud.tosca.model.templates.HaPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;

/**
 * Custom deserializer to handle multiple AbstractPolicy types.
 */
public class PolicyDeserializer extends AbstractFieldValueDiscriminatorPolymorphicDeserializer<AbstractPolicy> {
    public PolicyDeserializer() {
        super("type", AbstractPolicy.class);
        addToRegistry(HaPolicy.HA_POLICY, HaPolicy.class);
        addToRegistry(LocationPlacementPolicy.LOCATION_PLACEMENT_POLICY, LocationPlacementPolicy.class);
    }

    @Override
    protected AbstractPolicy deserializeAfterRead(JsonParser jp, DeserializationContext ctxt, ObjectMapper mapper, ObjectNode root) throws JsonProcessingException {
        AbstractPolicy result = super.deserializeAfterRead(jp, ctxt, mapper, root);
        if (result!=null) return result;

        // treat anything else as generic policy
        // all data is stored in the field data so extract that
        Map data = mapper.treeToValue(root, Map.class);
        if (data.containsKey("data")) data = (Map) data.get("data");
        return new GenericPolicy( data );
    }
}
