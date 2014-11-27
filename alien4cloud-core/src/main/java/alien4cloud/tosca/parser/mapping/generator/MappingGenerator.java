package alien4cloud.tosca.parser.mapping.generator;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.YamlSimpleParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import com.google.common.collect.Maps;

import java.nio.file.Paths;
import java.util.Map;

/**
 * Load type mapping definition from yaml and add it to the type mapping registry.
 */
public class MappingGenerator {

    public static void main(String[] args) throws ParsingException {
        MappingGenerator generator = new MappingGenerator();
        generator.load();
    }

    public void load() throws ParsingException {
        Map<String, INodeParser> registry = Maps.newHashMap();

        MappingNodeParser mappingParser = new MappingNodeParser();
        YamlSimpleParser<TypeNodeParser<?>> nodeParser = new YamlSimpleParser<>(mappingParser);
        ParsingResult<TypeNodeParser<?>> result = nodeParser.parseFile(Paths
                .get("/Users/lucboutier/Documents/workspace-fc/alien4cloud/alien4cloud-core/src/main/resources/tosca-simple-profile-wd03-mapping.yml"));

        registry.put(result.getResult().getToscaType(), result.getResult());
    }
}