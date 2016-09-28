package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import org.elasticsearch.common.collect.Maps;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.templates.SubstitutionMapping;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SustitutionMappingParser implements INodeParser<SubstitutionMapping> {
    private static final String NODE_TYPE = "node_type";
    private static final String CAPABILITIES = "capabilities";
    private static final String REQUIREMENTS = "requirements";

    @Resource
    private BaseParserFactory baseParserFactory;
    @Resource
    private ScalarParser scalarParser;

    private ListParser<String> stringListParser;

    @PostConstruct
    public void init() {
        stringListParser = baseParserFactory.getListParser(scalarParser, "string");
    }

    @Override
    public SubstitutionMapping parse(Node node, ParsingContextExecution context) {
        Topology topology = (Topology) context.getParent();

        if (!(node instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors().add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, node.getStartMark(), null, node.getEndMark(), null));
            return null;
        }
        SubstitutionMapping result = new SubstitutionMapping();
        MappingNode mappingNode = ((MappingNode) node);
        List<NodeTuple> nodeTuples = mappingNode.getValue();
        for (NodeTuple nodeTuple : nodeTuples) {
            String key = scalarParser.parse(nodeTuple.getKeyNode(), context);
            Node valueNode = nodeTuple.getValueNode();
            switch (key) {
            case NODE_TYPE:
                String nodeTypeName = scalarParser.parse(valueNode, context);
                NodeType nodeType = new NodeType();
                nodeType.setElementId(nodeTypeName);
                result.setSubstitutionType(nodeType);
                break;
            case CAPABILITIES:
                result.setCapabilities(parseSubstitutionTargets(valueNode, context));
                break;
            case REQUIREMENTS:
                result.setRequirements(parseSubstitutionTargets(valueNode, context));
                break;
            default:
                // FIXME add a warning
            }
        }
        return result;
    }

    private Map<String, SubstitutionTarget> parseSubstitutionTargets(Node valueNode, ParsingContextExecution context) {
        if (!(valueNode instanceof MappingNode)) {
            // we expect a MappingNode
            context.getParsingErrors()
                    .add(new ParsingError(ErrorCode.YAML_MAPPING_NODE_EXPECTED, null, valueNode.getStartMark(), null, valueNode.getEndMark(), null));
            return null;
        }
        Map<String, SubstitutionTarget> result = Maps.newHashMap();
        MappingNode mappingNode = ((MappingNode) valueNode);
        List<NodeTuple> nodeTuples = mappingNode.getValue();
        for (NodeTuple nodeTuple : nodeTuples) {
            String key = scalarParser.parse(nodeTuple.getKeyNode(), context);
            SubstitutionTarget target = parseSubstitutionTarget(nodeTuple.getValueNode(), context);
            if (target != null) {
                result.put(key, target);
            }
        }
        return result;
    }

    private SubstitutionTarget parseSubstitutionTarget(Node valueNode, ParsingContextExecution context) {
        List<String> values = (List<String>) stringListParser.parse(valueNode, context);
        if (values.size() != 2) {
            // FIXME: throw ex
            return null;
        }
        return new SubstitutionTarget(values.get(0), values.get(1));
    }
}