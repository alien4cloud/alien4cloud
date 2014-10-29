package alien4cloud.tosca.parser.impl;

import java.util.List;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.component.model.Tag;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContext;

import com.google.common.collect.Lists;

@Component
public class TagParser implements INodeParser<List<Tag>> {

    @Override
    public boolean isDeffered() {
        return false;
    }

    @Override
    public List<Tag> parse(Node node, ParsingContext context) {
        List<Tag> tagList = Lists.newArrayList();
        if (node instanceof MappingNode) {
            MappingNode mapNode = (MappingNode) node;
            for (NodeTuple entry : mapNode.getValue()) {
                String key = ParserUtils.getScalar(entry.getKeyNode(), context.getParsingErrors());
                String value = ParserUtils.getScalar(entry.getValueNode(), context.getParsingErrors());
                if (value != null) {
                    tagList.add(new Tag(key, value));
                }
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Alien Tag");
        }
        return tagList;
    }
}