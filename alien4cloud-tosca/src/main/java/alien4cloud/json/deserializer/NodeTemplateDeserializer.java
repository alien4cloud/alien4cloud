package alien4cloud.json.deserializer;

import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.jackson.ConditionalAttributes;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;

import java.lang.reflect.InvocationTargetException;

/**
 * Custom deserializer to handle multiple IOperationParameter types.
 */
public class NodeTemplateDeserializer extends AbstractDiscriminatorPolymorphicDeserializer<NodeTemplate> {

    public NodeTemplateDeserializer() {
        super(NodeTemplate.class);
        addToRegistry("serviceResourceId", ServiceNodeTemplate.class);
        setDefaultClass(NodeTemplate.class);
    }

}