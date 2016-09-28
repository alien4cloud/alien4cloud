package alien4cloud.json.deserializer;

import org.alien4cloud.tosca.model.definitions.AttributeDefinition;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;

/**
 * Custom deserializer to handle multiple AttributeValue types
 */
public class AttributeDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public AttributeDeserializer() {
        super(IValue.class);
        addToRegistry("type", AttributeDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
    }
}