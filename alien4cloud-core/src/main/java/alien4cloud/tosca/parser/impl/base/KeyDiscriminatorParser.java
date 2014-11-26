package alien4cloud.tosca.parser.impl.base;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.Map;
import java.util.Set;

/**
 * Map using a child parser based on a discriminator key (valid only for MappingNode).
 */
public class KeyDiscriminatorParser<T> implements INodeParser<T> {
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
            this.parserByExistKey = Maps.newHashMap();
        } else {
            this.parserByExistKey = parserByExistKey;
        }
        this.fallbackParser = fallbackParser;
    }

    @Override
    public T parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            // create a set of available keys
            MappingNode mappingNode = (MappingNode) node;
            Set<String> keySet = Sets.newHashSet();
            for (NodeTuple tuple : mappingNode.getValue()) {
                keySet.add(((ScalarNode) tuple.getKeyNode()).getValue());
            }
            // check if one of the discriminator key exists and if so use it for parsing.
            for (Map.Entry<String, INodeParser<T>> entry : parserByExistKey.entrySet()) {
                if (keySet.contains(entry.getKey())) {
                    return entry.getValue().parse(node, context);
                }
            }
        }
        return fallbackParser.parse(node, context);
    }

    @Override
    public boolean isDeferred() {
        return false;
    }
}
