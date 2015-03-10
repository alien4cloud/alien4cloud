package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

@Component
@Slf4j
public class OuputsParser extends DefaultDeferredParser<Void> {

    @Resource
    private CsarService csarService;

    @Resource
    private ScalarParser scalarParser;

    @Resource
    private ICSARRepositorySearchService searchService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Override
    public Void parse(Node node, ParsingContextExecution context) {
        Object parent = context.getParent();
        if (!(parent instanceof Topology)) {
            // TODO: throw ex
        }
        Topology topology = (Topology) parent;

        if (!(node instanceof MappingNode)) {
            context.getParsingErrors()
                    .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(),
                            null));
            return null;
        }
        MappingNode mappingNode = (MappingNode) node;

        Map<String, Set<String>> outputAttributes = null;
        Map<String, Set<String>> outputProperties = null;

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
                    if (params.size() != 2) {
                        // we need exactly 2 params to be able to do the job : node name & property or attribute name
                        context.getParsingErrors().add(
                                new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.OUTPUTS_BAD_PARAMS_COUNT, null, outputValueNode.getStartMark(), null,
                                        outputValueNode.getEndMark(), null));
                        continue;
                    }
                    String nodeTemplateName = params.get(0);
                    String nodeTemplatePropertyOrAttributeName = params.get(1);
                    // TODO: should we check they exist ?
                    switch(functionName) {
                    case "get_attribute":
                        outputAttributes = addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputAttributes);
                        break;
                    case "get_property":
                        outputProperties = addToMapOfSet(nodeTemplateName, nodeTemplatePropertyOrAttributeName, outputProperties);
                        break;
                    default:
                        context.getParsingErrors().add(
                                new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.OUTPUTS_UNKNOWN_FUNCTION, null, outputValueNode.getStartMark(), null,
                                        outputValueNode.getEndMark(), functionName));
                    }

                }
            }

        }

        topology.setOutputProperties(outputProperties);
        topology.setOutputAttributes(outputAttributes);

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

}