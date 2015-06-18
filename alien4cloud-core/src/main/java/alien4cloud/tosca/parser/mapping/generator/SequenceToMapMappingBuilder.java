package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.impl.base.SequenceToMapParser;

import com.google.common.collect.Maps;

/**
 * Build Mapping target for map.
 */
@Slf4j
@Component
public class SequenceToMapMappingBuilder implements IMappingBuilder {
    private static final String SEQUENCE_TO_MAP = "sequence_to_map";
    private static final String TYPE = "type";
    private static final String LIST_TYPE = "list_type";

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

        if (map.containsKey(TYPE) && map.containsKey(LIST_TYPE)) {
            log.warn("Both field <{}> and <{}> exist in your mapping. If it's defined <{}> will override the <{}>.", TYPE, LIST_TYPE, LIST_TYPE, TYPE);
        }
        // default mapping for simple type or for a list_type if defined
        SequenceToMapParser<Object> parser = new SequenceToMapParser<>(new ReferencedParser(map.get(TYPE)), map.get(TYPE));

        if (map.containsKey(LIST_TYPE) && !map.get(LIST_TYPE).isEmpty()) {
            ListParser listParser = new ListParser(new ReferencedParser(map.get(LIST_TYPE)), "sequence of " + map.get(LIST_TYPE));
            parser = new SequenceToMapParser<>(listParser, map.get(LIST_TYPE));
        }

        return new MappingTarget(map.get(SEQUENCE_TO_MAP), parser);
    }
}