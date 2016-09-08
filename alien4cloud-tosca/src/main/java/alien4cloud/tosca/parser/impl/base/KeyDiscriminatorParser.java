package alien4cloud.tosca.parser.impl.base;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;

/**
 * Map using a child parser based on a discriminator key (valid only for MappingNode).
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KeyDiscriminatorParser<T> implements INodeParser<T> {
    private static final String MAPPING_NODE_FALLBACK_KEY = "__";

    private Map<String, INodeParser<T>> parserByExistKey;
    private INodeParser<T> fallbackParser;

    /**
     * Create a new key discriminator parser instance.
     * 
     * @param parserByExistKey A map of existing keys to the parser to use in case the key exists.
     * @param fallbackParser The parser to use if none of the key is actually found or if the node type is not a MappingNode.
     */
    public KeyDiscriminatorParser(Map<String, INodeParser<T>> parserByExistKey, INodeParser<T> fallbackParser) {
        if (parserByExistKey == null) {
            this.parserByExistKey = Maps.newLinkedHashMap();
        } else {
            this.parserByExistKey = parserByExistKey;
        }
        this.fallbackParser = fallbackParser;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        Set<String> keySet = Sets.newHashSet();
        if (node instanceof MappingNode) {
            // create a set of available keys
            MappingNode mappingNode = (MappingNode) node;
            for (NodeTuple tuple : mappingNode.getValue()) {
                keySet.add(((ScalarNode) tuple.getKeyNode()).getValue());
            }
            INodeParser<T> mappingNodeFallbackParser = null;
            // check if one of the discriminator key exists and if so use it for parsing.
            for (Map.Entry<String, INodeParser<T>> entry : parserByExistKey.entrySet()) {
                if (keySet.contains(entry.getKey())) {
                    return entry.getValue().parse(node, context);
                } else if (MAPPING_NODE_FALLBACK_KEY.equals(entry.getKey())) {
                    mappingNodeFallbackParser = entry.getValue();
                }
            }

            // if not we should use the mapping node fallback parser.
            if (mappingNodeFallbackParser != null) {
                return mappingNodeFallbackParser.parse(node, context);
            }
        }
        if (fallbackParser != null) {
            return fallbackParser.parse(node, context);
        } else {
            context.getParsingErrors().add(new ParsingError(ErrorCode.UNKNWON_DISCRIMINATOR_KEY, "Invalid scalar value.", node.getStartMark(),
                    "Tosca type cannot be expressed with the given scalar value.", node.getEndMark(), keySet.toString()));
        }
        return null;
    }
}