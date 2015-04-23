package alien4cloud.json.deserializer;

import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.HaPolicy;

/**
 * Custom deserializer to handle multiple AbstractPolicy types.
 */
public class PolicyDeserializer extends AbstractFieldValueDiscriminatorPolymorphicDeserializer<AbstractPolicy> {
    public PolicyDeserializer() {
        super("type", AbstractPolicy.class);
        addToRegistry(HaPolicy.HA_POLICY, HaPolicy.class);
    }

}