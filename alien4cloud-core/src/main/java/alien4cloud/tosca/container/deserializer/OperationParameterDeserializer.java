package alien4cloud.tosca.container.deserializer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import alien4cloud.tosca.model.FunctionPropertyValue;
import alien4cloud.tosca.model.IOperationParameter;
import alien4cloud.tosca.model.PropertyDefinition;
import alien4cloud.tosca.model.ScalarPropertyValue;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

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