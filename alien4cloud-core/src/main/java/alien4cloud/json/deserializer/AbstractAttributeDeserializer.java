package alien4cloud.json.deserializer;

import alien4cloud.model.components.AttributeDefinition;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.IValue;

/**
 * Custom deserializer to handle multiple AttributeValue types
 */
public class AbstractAttributeDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public AbstractAttributeDeserializer() {
        super(IValue.class);
        addToRegistry("type", AttributeDefinition.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
    }
}