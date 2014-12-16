package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
@Component
public class ListMappingBuilder implements IMappingBuilder {
    private static final String LIST = "list";
    private static final String TYPE = "type";
    private static final String KEY = "key";

    @Override
    public String getKey() {
        return LIST;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = Maps.newHashMap();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = ParserUtils.getScalar(tuple.getKeyNode(), context);
            String value = ParserUtils.getScalar(tuple.getValueNode(), context);
            map.put(key, value);
        }
        ListParser parser;
        if (map.get(KEY) == null) {
            parser = new ListParser(new ReferencedParser(map.get(TYPE)), "sequence of " + map.get(TYPE));
        } else {
            parser = new ListParser(new ReferencedParser(map.get(TYPE)), "sequence of " + map.get(TYPE), map.get(KEY));
        }
        return new MappingTarget(map.get(LIST), parser);
    }
}