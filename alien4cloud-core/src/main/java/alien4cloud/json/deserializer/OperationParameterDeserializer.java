package alien4cloud.json.deserializer;

import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class OperationParameterDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public OperationParameterDeserializer() {
        super(IValue.class);
        addToRegistry("type", PropertyDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("value", ScalarPropertyValue.class);
    }
}