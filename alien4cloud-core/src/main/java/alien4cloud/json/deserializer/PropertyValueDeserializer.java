package alien4cloud.json.deserializer;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.ListPropertyValue;
import alien4cloud.model.components.ScalarPropertyValue;

import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class PropertyValueDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractPropertyValue> {
    public PropertyValueDeserializer() {
        super(AbstractPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING, ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY, ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT, ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }
}