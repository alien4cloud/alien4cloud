package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
@Component
public class MapMappingBuilder implements IMappingBuilder {
    private static final String MAP = "map";
    private static final String TYPE = "type";
    private static final String KEY = "key";

    @Override
    public String getKey() {
        return MAP;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }

        return new MappingTarget(map.get(MAP), new MapParser<IndexedNodeType>(new ReferencedParser(map.get(TYPE)), "map of " + map.get(TYPE), map.get(KEY)));
    }
}