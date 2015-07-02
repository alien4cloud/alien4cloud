package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.SequenceToMapParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
@Component
public class SequenceToMapMappingBuilder implements IMappingBuilder {
    private static final String SEQUENCE_TO_MAP = "sequence_to_map";
    private static final String TYPE = "type";
    // true if the node from the sequence contains both the key of the map (from first tuple key) and the value of the map (based on mapping node parsing)
    // false if the node from the sequence contains only the key of the map and the value of the map should be parsed from the value of the first tuple.
    private static final String NODE_IS_VALUE = "node_is_value";

    @Override
    public String getKey() {
        return SEQUENCE_TO_MAP;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        if(map.get(NODE_IS_VALUE) == null) {
            map.put(NODE_IS_VALUE, "true");
        }
        return new MappingTarget(map.get(SEQUENCE_TO_MAP), new SequenceToMapParser<>(new ReferencedParser(map.get(TYPE)), map.get(TYPE),
                Boolean.parseBoolean(map.get(NODE_IS_VALUE))));
    }
}