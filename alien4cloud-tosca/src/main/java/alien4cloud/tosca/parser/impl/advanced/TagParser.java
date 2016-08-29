package alien4cloud.tosca.parser.impl.advanced;

import java.util.List;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Lists;

import alien4cloud.model.common.Tag;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class TagParser implements INodeParser<List<Tag>> {

    @Override
    public List<Tag> parse(Node node, ParsingContextExecution context) {
        List<Tag> tagList = Lists.newArrayList();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = ParserUtils.getScalar(entry.getKeyNode(), context);
                String value = ParserUtils.getScalar(entry.getValueNode(), context);
                if (value != null) {
                    tagList.add(new Tag(key, value));
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "metadata");
        }
        return tagList;
    }
}