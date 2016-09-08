package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Build Mapping target for map.
 */
@Slf4j
@Component
public class MapMappingBuilder implements IMappingBuilder {
    private static final String MAP = "map";
    private static final String TYPE = "type";
    private static final String KEY = "key";
    private static final String LIST_TYPE = "list_type";

    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public String getKey() {
        return MAP;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = ParserUtils.parseStringMap(mappingNode, context);
        if (map.containsKey(TYPE) && map.containsKey(LIST_TYPE)) {
            log.warn("Both field <{}> and <{}> exist in your mapping. If it's defined <{}> will override the <{}>.", TYPE, LIST_TYPE, LIST_TYPE, TYPE);
        }
        // default mapping for simple type or for a list_type if defined
        MappingTarget mappingTarget = new MappingTarget(map.get(MAP),
                baseParserFactory.getMapParser(baseParserFactory.getReferencedParser(map.get(TYPE)), "map of " + map.get(TYPE), map.get(KEY)));
        if (map.containsKey(LIST_TYPE) && !map.get(LIST_TYPE).isEmpty()) {
            mappingTarget = new MappingTarget(map.get(MAP), baseParserFactory.getMapParser(
                    baseParserFactory.getListParser(baseParserFactory.getReferencedParser(map.get(TYPE)), "list of " + map.get(LIST_TYPE), map.get(KEY)),
                    "map of " + map.get(LIST_TYPE), map.get(KEY)));
        }

        return mappingTarget;
    }
}