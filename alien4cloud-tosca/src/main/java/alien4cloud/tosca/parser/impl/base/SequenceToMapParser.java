package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.*;

import alien4cloud.tosca.parser.*;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.AllArgsConstructor;

/**
 * Parse a yaml sequence into a {@link Map}
 *
 * @param <T> The type of the values of the map.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@AllArgsConstructor
public class SequenceToMapParser<T> implements INodeParser<Map<String, T>> {
    private INodeParser<T> valueParser;
    /** The tosca type of the map. */
    private String toscaType;
    /** If the sequence element mapping node is also the */
    private Boolean nodeIsValue;
    /** If we should generate keys for duplicated elements or trigger errors. */
    private Boolean allowDuplicate;

    @Override
    public Map<String, T> parse(Node node, ParsingContextExecution context) {
        if (node instanceof SequenceNode) {
            SequenceNode sequenceNode = (SequenceNode) node;
            Map<String, T> sequenceMap = Maps.newLinkedHashMap();
            for (Node elementNode : sequenceNode.getValue()) {
                if (elementNode instanceof MappingNode) {
                    MappingNode mappingNode = (MappingNode) elementNode;
                    String key = ((ScalarNode) mappingNode.getValue().get(0).getKeyNode()).getValue();
                    T value;
                    if (nodeIsValue) {
                        value = valueParser.parse(mappingNode, context);
                    } else {
                        value = valueParser.parse(mappingNode.getValue().get(0).getValueNode(), context);
                        checkMappingNodeSingleProperty(mappingNode, context);
                    }
                    if (value == null) {
                        ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.SYNTAX_ERROR,
                                "Invalid format for the value.", node.getStartMark(), "The value cannot be parsed", node.getEndMark(),
                                key);
                        context.getParsingErrors().add(err);
                    } else if (allowDuplicate) {
                        sequenceMap.put(getUniqueKey(sequenceMap, key), value);
                    } else if (sequenceMap.containsKey(key)) {
                        ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.DUPLICATED_ELEMENT_DECLARATION,
                                "Key in the sequence must be unique.", node.getStartMark(), "The value of this tuple should be a scalar", node.getEndMark(),
                                key);
                        context.getParsingErrors().add(err);
                    } else {
                        sequenceMap.put(key, value);
                    }
                } else {
                    ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
                }
            }

            return sequenceMap;
        }

        ParserUtils.addTypeError(node, context.getParsingErrors(), toscaType);
        return null;
    }

    private String getUniqueKey(Map<String, T> sequenceMap, String key) {
        int increment = 0;
        String uniqueKey = key;
        while (sequenceMap.containsKey(uniqueKey)) {
            uniqueKey = key + "_" + increment;
            increment++;
        }
        return uniqueKey;
    }

    /**
     * Check that the mapping node has a single property (when value is node is false other properties are ignored).
     * 
     * @param mappingNode The mapping node.
     * @param context The parsing context.
     */
    public void checkMappingNodeSingleProperty(MappingNode mappingNode, ParsingContextExecution context) {
        if (mappingNode.getValue().size() > 1) {
            // some properties are ignored.
            for (int i = 1; i < mappingNode.getValue().size(); i++) {
                NodeTuple entry = mappingNode.getValue().get(i);
                ParsingError err = new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNRECOGNIZED_PROPERTY, "Parsing a MappingNode as a Map",
                        entry.getKeyNode().getStartMark(), "The value of this tuple should be a scalar", entry.getValueNode().getEndMark(),
                        ((ScalarNode) entry.getKeyNode()).getValue());
                context.getParsingErrors().add(err);
            }
        }
    }
}