package alien4cloud.tosca.container.deserializer;

import alien4cloud.tosca.model.AbstractPropertyValue;
import alien4cloud.tosca.model.FunctionPropertyValue;
import alien4cloud.tosca.model.ScalarPropertyValue;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class PropertyValueDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractPropertyValue> {
    public PropertyValueDeserializer() {
        super(AbstractPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("value", ScalarPropertyValue.class);
    }
}