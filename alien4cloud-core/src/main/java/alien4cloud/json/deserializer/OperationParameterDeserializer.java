package alien4cloud.json.deserializer;

import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class OperationParameterDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IOperationParameter> {
    public OperationParameterDeserializer() {
        super(IOperationParameter.class);
        addToRegistry("type", PropertyDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("value", ScalarPropertyValue.class);
    }
}