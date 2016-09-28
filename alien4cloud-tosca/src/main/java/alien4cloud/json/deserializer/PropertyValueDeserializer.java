package alien4cloud.json.deserializer;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;

import com.fasterxml.jackson.databind.node.JsonNodeType;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class PropertyValueDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractPropertyValue> {
    public PropertyValueDeserializer() {
        super(AbstractPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY.toString(), ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT.toString(), ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }
}