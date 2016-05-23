package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;

import com.google.common.collect.Maps;

/**
 * Mapping of a type reference.
 */
@Component
public class ReferenceMappingBuilder implements IMappingBuilder {

    private static final String REFERENCE_KEY = "reference";

    private static final String TYPE = "type";

    private static final String DEFERRED = "deferred";

    private static final String DEFERRED_ORDER = "deferredOrder";

    @Override
    public String getKey() {
        return REFERENCE_KEY;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        boolean deferred = map.containsKey(DEFERRED) && Boolean.parseBoolean(map.get(DEFERRED));
        int deferredOrder = map.containsKey(DEFERRED_ORDER) ? Integer.parseInt(map.get(DEFERRED_ORDER)) : 0;
        return new MappingTarget(map.get(REFERENCE_KEY), new ReferencedParser(map.get(TYPE), deferred, deferredOrder));
    }
}