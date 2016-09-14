package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;

import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.tosca.parser.*;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;

/**
 * Parse a relationship
 */
@Component
public class RelationshipTemplateParser implements INodeParser<RelationshipTemplate> {
    @Resource
    private ScalarParser scalarParser;
    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public RelationshipTemplate parse(Node node, ParsingContextExecution context) {
        // To parse a relationship template we actually get the parent node to retrieve the requirement name;
        if (!(node instanceof MappingNode) || ((MappingNode) node).getValue().size() != 1) {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Requirement assignment");
        }
        MappingNode assignmentNode = (MappingNode) node;
        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        relationshipTemplate.setRequirementName(scalarParser.parse(assignmentNode.getValue().get(0).getKeyNode(), context));

        // Now parse the content of the relationship assignment.
        node = assignmentNode.getValue().get(0).getValueNode();
        if (node instanceof ScalarNode) { // Short notation (host: compute)
            relationshipTemplate.setTarget(scalarParser.parse(node, context));
        } else if (node instanceof MappingNode) {

            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple nodeTuple : mappingNode.getValue()) {
                String key = scalarParser.parse(nodeTuple.getKeyNode(), context);
                switch (key) {
                case "node":
                    relationshipTemplate.setTarget(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "capability":
                    relationshipTemplate.setTargetedCapabilityName(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "relationship":
                    relationshipTemplate.setType(scalarParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "properties":
                    INodeParser<AbstractPropertyValue> propertyValueParser = context.getRegistry().get("node_template_property");
                    MapParser<AbstractPropertyValue> mapParser = baseParserFactory.getMapParser(propertyValueParser, "node_template_property");
                    relationshipTemplate.setProperties(mapParser.parse(nodeTuple.getValueNode(), context));
                    break;
                case "interfaces":
                    INodeParser<Map<String, Interface>> interfacesParser = context.getRegistry().get("interfaces");
                    relationshipTemplate.setInterfaces(interfacesParser.parse(nodeTuple.getValueNode(), context));
                    break;
                default:
                    context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_ARTIFACT_KEY, null, node.getStartMark(),
                            "Unrecognized key while parsing implementation artifact", node.getEndMark(), key));
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Requirement assignment");
        }

        return relationshipTemplate;
    }
}