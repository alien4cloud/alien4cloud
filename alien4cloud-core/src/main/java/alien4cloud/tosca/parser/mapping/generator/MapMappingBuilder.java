package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
public class MapMappingBuilder implements IMappingBuilder {
    private static final String MAP_KEY = "map";

    @Override
    public String getKey() {
        return MAP_KEY;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        return new MappingTarget(map.get(MAP_KEY), new MapParser<IndexedNodeType>(new ReferencedParser(map.get("type")), "map of " + map.get("type"),
                map.get("key")));
    }
}