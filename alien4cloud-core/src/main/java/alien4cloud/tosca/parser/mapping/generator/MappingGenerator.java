package alien4cloud.tosca.parser.mapping.generator;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.YamlSimpleParser;

import com.google.common.collect.Maps;

/**
 * Load type mapping definition from yaml and add it to the type mapping registry.
 */
@Component
public class MappingGenerator {
    @Resource
    private ApplicationContext applicationContext;

    private Map<String, INodeParser> parsers = Maps.newHashMap();
    private Map<String, IMappingBuilder> mappingBuilders = Maps.newHashMap();

    public void initialize() {
        Map<String, INodeParser> contextParsers = applicationContext.getBeansOfType(INodeParser.class);
        // register parsers based on their class name.
        for (INodeParser parser : contextParsers.values()) {
            parsers.put(parser.getClass().getName(), parser);
        }
        Map<String, IMappingBuilder> contextMappingBuilders = applicationContext.getBeansOfType(IMappingBuilder.class);
        for (IMappingBuilder mappingBuilder : contextMappingBuilders.values()) {
            mappingBuilders.put(mappingBuilder.getKey(), mappingBuilder);
        }
    }

    public void process() throws ParsingException {
        MappingNodeParserImpl mappingParser = new MappingNodeParserImpl();
        YamlSimpleParser<List<INodeParser<?>>> nodeParser = new YamlSimpleParser<>(mappingParser);
        ParsingResult<List<INodeParser<?>>> result = nodeParser.parseFile(Paths
                .get("/Users/lucboutier/Documents/workspace-fc/alien4cloud/alien4cloud-core/src/main/resources/tosca-simple-profile-wd03-mapping.yml"));
    }
}