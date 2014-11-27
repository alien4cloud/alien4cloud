package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.model.Interface;
import alien4cloud.tosca.model.Operation;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.Wd03OperationDefinition;

import com.google.common.collect.Maps;

@Component
public class InterfaceParser implements INodeParser<Interface> {
    private static final String INPUTS_KEY = "inputs";
    private static final String DESCRIPTION_KEY = "description";

    @Resource
    private Wd03OperationDefinition operationDefinition;
    @Resource
    private ImplementationArtifactParser implementationArtifactParser;

    @Override
    public Interface parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return parseInterfaceDefinition((MappingNode) node, context);
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Interface definition");
        }
        return null;
    }

    private Interface parseInterfaceDefinition(MappingNode node, ParsingContextExecution context) {
        Interface interfaz = new Interface();
        Map<String, Operation> operations = Maps.newHashMap();
        interfaz.setOperations(operations);

        for (NodeTuple entry : node.getValue()) {
            String key = ParserUtils.getScalar(entry.getKeyNode(), context.getParsingErrors());
            if (INPUTS_KEY.equals(key)) {
                // TODO process inputs.
            } else if (DESCRIPTION_KEY.equals(key)) {
                if (entry.getValueNode() instanceof ScalarNode) {
                    interfaz.setDescription(((ScalarNode) entry.getValueNode()).getValue());
                } else {
                    ParserUtils.addTypeError(node, context.getParsingErrors(), "Interface description");
                }
            } else {
                if (entry.getValueNode() instanceof ScalarNode) {
                    Operation operation = new Operation();
                    operation.setImplementationArtifact(implementationArtifactParser.parse(entry.getValueNode(), context));
                    operations.put(key, operation);
                } else {
                    operations.put(key, operationDefinition.getParser().parse(entry.getValueNode(), context));
                }
            }
        }
        return interfaz;
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return false;
    }
}