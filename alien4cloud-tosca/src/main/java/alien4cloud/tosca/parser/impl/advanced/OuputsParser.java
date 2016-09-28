package alien4cloud.tosca.parser.impl.advanced;

import java.util.*;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OuputsParser implements INodeParser<Void> {
    @Override
    public Void parse(Node node, ParsingContextExecution context) {
        Topology topology = (Topology) context.getParent();

        if (!(node instanceof MappingNode)) {
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null,
                    node.getEndMark(), null));
            return null;
        }
        MappingNode mappingNode = (MappingNode) node;

        Map<String, Set<String>> outputAttributes = null;
        Map<String, Set<String>> outputProperties = null;
        Map<String, Map<String, Set<String>>> ouputCapabilityProperties = null;

        List<NodeTuple> children = mappingNode.getValue();
        for (NodeTuple child : children) {
            Node childValueNode = child.getValueNode();
            if (!(childValueNode instanceof MappingNode)) {
                // not a mapping jut ignore the entry
                continue;
            }
            for (NodeTuple childChild : ((MappingNode) childValueNode).getValue()) {
                if (childChild.getKeyNode() instanceof ScalarNode && ((ScalarNode) childChild.getKeyNode()).getValue().equals("value")) {
                    // we are only interested by the 'value' node
                    Node outputValueNode = childChild.getValueNode();
                    // now we have to parse this node
                    INodeParser<?> p = context.getRegistry().get("tosca_function");
                    FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) p.parse(outputValueNode, context);
                    String functionName = functionPropertyValue.getFunction();
                    List<String> params = functionPropertyValue.getParameters();
                    if (params.size() == 2) {
                        // we need exactly 2 params to be able to do the job : node name & property or attribute name
                        String nodeTemplateName = params.get(0);
                        String nodeTemplatePropertyOrAttributeName = params.get(1);
                        // TODO: should we check they exist ?
                        switch (functionName) {
                        case "get_attribute":
                            outputAttributes = addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputAttributes);
                            break;
                        case "get_property":
                            outputProperties = addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputProperties);
                            break;
                        default:
                            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.OUTPUTS_UNKNOWN_FUNCTION, null,
                                    outputValueNode.getStartMark(), null, outputValueNode.getEndMark(), functionName));
                        }
                    } else if (params.size() == 3 && functionName.equals("get_property")) {
                        // in case of 3 parameters we only manage capabilities outputs for the moment
                        String nodeTemplateName = params.get(0);
                        String capabilityName = params.get(1);
                        String propertyName = params.get(2);
                        ouputCapabilityProperties = addToMapOfMapOfSet(nodeTemplateName, capabilityName, propertyName, ouputCapabilityProperties);
                    } else {
                        context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.OUTPUTS_BAD_PARAMS_COUNT, null,
                                outputValueNode.getStartMark(), null, outputValueNode.getEndMark(), null));
                    }

                }
            }

        }

        topology.setOutputProperties(outputProperties);
        topology.setOutputAttributes(outputAttributes);
        topology.setOutputCapabilityProperties(ouputCapabilityProperties);

        return null;
    }

    private Map<String, Set<String>> addToMapOfSet(String key, String value, Map<String, Set<String>> map) {
        if (map == null) {
            map = new HashMap<String, Set<String>>();
        }
        Set<String> set = map.get(key);
        if (set == null) {
            set = new HashSet<String>();
            map.put(key, set);
        }
        set.add(value);
        return map;
    }

    private Map<String, Map<String, Set<String>>> addToMapOfMapOfSet(String key1, String key2, String value, Map<String, Map<String, Set<String>>> map) {
        if (map == null) {
            map = new HashMap<String, Map<String, Set<String>>>();
        }
        Map<String, Set<String>> map1 = map.get(key1);
        map.put(key1, addToMapOfSet(key2, value, map1));
        return map;
    }

}