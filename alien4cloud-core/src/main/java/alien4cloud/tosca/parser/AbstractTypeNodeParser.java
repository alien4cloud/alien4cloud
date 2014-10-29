package alien4cloud.tosca.parser;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.SequenceNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Abstract class to work with Type Node Parsing.
 */
public abstract class AbstractTypeNodeParser {

    protected void parseAndSetValue(BeanWrapper target, Node valueNode, ParsingContext context, String path, IParser parser) {
        Class<?> clazz = target.getPropertyType(path);
        if (valueNode instanceof MappingNode && Map.class.isAssignableFrom(clazz)) {
            parseAndSetMap(target, (MappingNode) valueNode, context, path, parser);
        } else if (valueNode instanceof SequenceNode) {
            parseAndSetSequence(target, (SequenceNode) valueNode, context, path, parser);
        } else {
            // find the target field and set value
            Object value = ((INodeParser<?>) parser).parse(valueNode, context);
            target.setPropertyValue(path, value);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void parseAndSetMap(BeanWrapper target, MappingNode valueNode, ParsingContext context, String path, IParser parser) {
        // ensure the map is initialized
        Map map = (Map) target.getPropertyValue(path);
        if (map == null) {
            map = Maps.newHashMap();
            target.setPropertyValue(path, map);
        }

        for (NodeTuple entry : valueNode.getValue()) {
            String key = ParserUtils.getScalar(entry.getKeyNode(), context.getParsingErrors());
            Object value;
            if (parser instanceof INodeParser) {
                value = ((INodeParser<?>) parser).parse(entry.getValueNode(), context);
            } else {
                value = ((INodeTupleParser<?>) parser).parse(entry, context);
            }
            if (value != null) {
                map.put(key, value);
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void parseAndSetSequence(BeanWrapper target, SequenceNode valueNode, ParsingContext context, String path, IParser parser) {
        List<Node> valueNodes = valueNode.getValue();
        Collection collection = (Collection) target.getPropertyValue(path);
        if (collection == null) {
            Class<?> sequenceFieldType = target.getPropertyType(path);
            if (List.class.isAssignableFrom(sequenceFieldType)) {
                collection = Lists.newArrayList();
                target.setPropertyValue(path, collection);
            } else if (Set.class.isAssignableFrom(sequenceFieldType)) {
                collection = Sets.newHashSet();
                target.setPropertyValue(path, collection);
            } else {
                context.getParsingErrors().add(new ToscaParsingError(false, null, "", valueNode.getStartMark(), "", valueNode.getEndMark(), ""));
            }
        }
        for (Node node : valueNodes) {
            Object value = ((INodeParser<?>) parser).parse(node, context);
            if (value != null) {
                collection.add(value);
            }
        }
    }
}