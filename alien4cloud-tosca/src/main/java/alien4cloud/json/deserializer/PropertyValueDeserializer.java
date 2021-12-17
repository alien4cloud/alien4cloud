package alien4cloud.json.deserializer;

import java.lang.reflect.InvocationTargetException;

import org.alien4cloud.tosca.model.definitions.*;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.jackson.ConditionalAttributes;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class PropertyValueDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<AbstractPropertyValue> {
    public PropertyValueDeserializer() {
        super(AbstractPropertyValue.class);
        addToRegistry("function_concat", ConcatPropertyValue.class);
        addToRegistry("function_token", TokenPropertyValue.class);
        addToRegistry("function", FunctionPropertyValue.class);
        // let's handle null with a scalar deserializer.
        addToRegistry("value", JsonNodeType.NULL.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.STRING.toString(), ScalarPropertyValue.class);
        addToRegistry("value", JsonNodeType.ARRAY.toString(), ListPropertyValue.class);
        addToRegistry("value", JsonNodeType.OBJECT.toString(), ComplexPropertyValue.class);
        setValueStringClass(ScalarPropertyValue.class);
    }

    @Override
    public AbstractPropertyValue getNullValue(DeserializationContext ctxt) throws JsonMappingException {
        if (ctxt.getAttribute(ConditionalAttributes.REST) != null && RestMapper.PATCH.equals(RestMapper.REQUEST_OPERATION.get())) {
            try {
                AbstractPropertyValue instance = (AbstractPropertyValue) RestMapper.NULL_INSTANCES.get(ScalarPropertyValue.class);
                if (instance == null) {
                    instance = ScalarPropertyValue.class.getConstructor().newInstance();
                }
                RestMapper.NULL_INSTANCES.put(ScalarPropertyValue.class, instance);
                return instance;
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            }
        }
        return super.getNullValue(ctxt);
    }
}
