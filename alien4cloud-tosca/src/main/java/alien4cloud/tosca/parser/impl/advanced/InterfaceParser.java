package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

@Component
public class InterfaceParser implements INodeParser<Interface> {
    private static final String INPUTS_KEY = "inputs";
    private static final String TYPE_KEY = "type";
    private static final String DESCRIPTION_KEY = "description";

    @Resource
    private ScalarParser scalarParser;
    @Resource
    private ImplementationArtifactParser implementationArtifactParser;
    @Resource
    private BaseParserFactory baseParserFactory;
    private ReferencedParser<Operation> operationParser;
    private MapParser<IValue> inputsParser;

    @PostConstruct
    public void init() {
        operationParser = baseParserFactory.getReferencedParser("operation_definition");
        inputsParser = new MapParser<IValue>(baseParserFactory.getReferencedParser("input"), "Interface Inputs");
        inputsParser.setScalarParser(scalarParser);
    }

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
        Map<String, IValue> interfaceInputs = null;

        for (NodeTuple entry : node.getValue()) {
            String key = scalarParser.parse(entry.getKeyNode(), context);
            if (INPUTS_KEY.equals(key)) {
                interfaceInputs = inputsParser.parse(entry.getValueNode(), context);
            } else if (DESCRIPTION_KEY.equals(key)) {
                interfaz.setDescription(scalarParser.parse(entry.getValueNode(), context));
            } else if (TYPE_KEY.equals(key)) {
                interfaz.setType(getInterfaceType(scalarParser.parse(entry.getValueNode(), context)));
            } else {
                if (entry.getValueNode() instanceof ScalarNode) {
                    Operation operation = new Operation();
                    // implementation artifact parsing should be done using a deferred parser as we need to look for artifact types.
                    operation.setImplementationArtifact(implementationArtifactParser.parse(entry.getValueNode(), context));
                    operations.put(key, operation);
                } else {
                    operations.put(key, operationParser.parse(entry.getValueNode(), context));
                }
            }
        }

        if (interfaceInputs != null) {
            // set this inputs to the interface for merge with type hierarchy usage
            interfaz.setInputParameters(interfaceInputs);
            for (Operation operation : operations.values()) {
                if (operation.getInputParameters() == null) {
                    operation.setInputParameters(Maps.newHashMap());
                }
                for (Entry<String, IValue> inputEntry : interfaceInputs.entrySet()) {
                    if (!operation.getInputParameters().containsKey(inputEntry.getKey())) {
                        operation.getInputParameters().put(inputEntry.getKey(), inputEntry.getValue());
                    }
                }
            }
        }

        return interfaz;
    }

    /**
     * FIXME PUT THAT SOMEWHERE MORE GLOBAL
     * 
     * @param interfaceType
     * @return
     */
    public static String getInterfaceType(String interfaceType) {
        if (ToscaNodeLifecycleConstants.STANDARD_SHORT.equalsIgnoreCase(interfaceType)) {
            return ToscaNodeLifecycleConstants.STANDARD;
        } else if (ToscaRelationshipLifecycleConstants.CONFIGURE_SHORT.equalsIgnoreCase(interfaceType)) {
            return ToscaRelationshipLifecycleConstants.CONFIGURE;
        }
        return interfaceType;
    }

}