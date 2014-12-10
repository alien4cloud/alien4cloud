package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.SetParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
public class SetMappingBuilder implements IMappingBuilder {
    private static final String SET_KEY = "set";

    @Override
    public String getKey() {
        return SET_KEY;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        SetParser parser;
        if (map.get("key") == null) {
            parser = new SetParser(new ReferencedParser(map.get("type")), "sequence of " + map.get("type"));
        } else {
            parser = new SetParser(new ReferencedParser(map.get("type")), "map of " + map.get("type"), map.get("key"));
        }
        return new MappingTarget(map.get(SET_KEY), parser);
    }
}