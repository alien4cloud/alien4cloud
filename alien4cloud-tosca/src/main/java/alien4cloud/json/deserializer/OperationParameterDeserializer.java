package alien4cloud.json.deserializer;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.alien4cloud.tosca.model.definitions.*;

/**
 * Custom deserializer to handle multiple operation parameters types.
 */
public class OperationParameterDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<IValue> {
    public OperationParameterDeserializer() {
        super(IValue.class);
        addToRegistry("type", PropertyDefinition.class);
        addToRegistry("function", FunctionPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("function_token", TokenPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY.toString(), ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT.toString(), ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }
}
