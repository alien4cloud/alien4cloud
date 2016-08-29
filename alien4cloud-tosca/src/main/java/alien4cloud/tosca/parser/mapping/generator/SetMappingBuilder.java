package alien4cloud.tosca.parser.mapping.generator;

import java.util.Map;

import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.SetParser;

import javax.annotation.Resource;

/**
 * Build Mapping target for map.
 */
@Component
public class SetMappingBuilder implements IMappingBuilder {
    private static final String SET = "set";
    private static final String TYPE = "type";
    private static final String KEY = "key";

    @Resource
    private BaseParserFactory baseParserFactory;

    @Override
    public String getKey() {
        return SET;
    }

    @Override
    public MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context) {
        Map<String, String> map = ParserUtils.parseStringMap(mappingNode, context);
        SetParser parser;
        if (map.get(KEY) == null) {
            parser = baseParserFactory.getSetParser(baseParserFactory.getReferencedParser(map.get(TYPE)), "sequence of " + map.get(TYPE));
        } else {
            parser = baseParserFactory.getSetParser(baseParserFactory.getReferencedParser(map.get(TYPE)), "map of " + map.get(TYPE), map.get(KEY));
        }
        return new MappingTarget(map.get(SET), parser);
    }
}