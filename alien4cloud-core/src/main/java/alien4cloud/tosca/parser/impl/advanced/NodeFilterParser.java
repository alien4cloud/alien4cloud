package alien4cloud.tosca.parser.impl.advanced;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.model.components.NodeFilter;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;

import com.google.common.collect.Lists;

@Component
public class NodeFilterParser extends DefaultParser<NodeFilter> {

    private static final String[] KEYS = { "properties", "capabilities" };

    @Resource
    private ConstraintParser constraintParser;

    @Override
    public NodeFilter parse(Node node, ParsingContextExecution context) {

        NodeFilter nodeFilter = new NodeFilter();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = ParserUtils.getScalar(entry.getKeyNode(), context);
                if (Arrays.asList(KEYS).contains(key)) {
                    Node valueNode = entry.getValueNode();
                    if (valueNode instanceof SequenceNode) {
                        List<Node> values = ((SequenceNode) valueNode).getValue();
                        for (Node property : values) {
                            List<NodeTuple> propertyTuples = ((MappingNode) property).getValue();
                            for (NodeTuple prop : propertyTuples) {
                                String propertyKey = ParserUtils.getScalar(prop.getKeyNode(), context);
                                PropertyConstraint constraintProperty = constraintParser.parse(prop.getValueNode(), context);
                                ArrayList<PropertyConstraint> list = Lists.newArrayList();
                                list.add(constraintProperty);
                                nodeFilter.getProperties().put(propertyKey, list);
                            }
                        }
                    }

                } else {
                    ParserUtils.addTypeError(node, context.getParsingErrors(), "Property NodeFilter field");
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Property NodeFilter");
        }

        return nodeFilter;

    }
}
