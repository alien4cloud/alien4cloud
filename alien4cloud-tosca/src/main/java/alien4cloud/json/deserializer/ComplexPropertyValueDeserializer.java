package alien4cloud.json.deserializer;


import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;

@Slf4j
public class ComplexPropertyValueDeserializer extends AbstractTreeDiscriminatorPolymorphicDeserializer {

    public ComplexPropertyValueDeserializer() {
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("function_token", ConcatPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
    }
}
