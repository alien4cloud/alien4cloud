package alien4cloud.json.deserializer;

import org.alien4cloud.tosca.model.definitions.*;

/**
 * Custom deserializer to handle multiple AttributeValue types
 */
public class AttributeDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public AttributeDeserializer() {
        super(IValue.class);
        addToRegistry("type", AttributeDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("function_token", TokenPropertyValue.class);
    }
}
