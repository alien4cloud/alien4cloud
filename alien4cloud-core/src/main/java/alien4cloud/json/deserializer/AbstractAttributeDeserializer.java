package alien4cloud.json.deserializer;

import alien4cloud.model.components.AttributeDefinition;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.IAttributeValue;

/**
 * Custom deserializer to handle multiple AttributeValue types
 */
public class AbstractAttributeDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IAttributeValue> {
    public AbstractAttributeDeserializer() {
        super(IAttributeValue.class);
        addToRegistry("type", AttributeDefinition.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
    }
}