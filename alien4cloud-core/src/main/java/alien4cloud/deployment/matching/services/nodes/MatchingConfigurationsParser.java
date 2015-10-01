package alien4cloud.deployment.matching.services.nodes;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.YamlParser;
import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;

/**
 * Parse a matching configuration from a yaml format.
 */
@Component
public class MatchingConfigurationsParser extends YamlParser<MatchingConfigurations> {
    private static final String MATCHING_CONFIGURATION_TYPE = "matching_configurations";

    @Inject
    private MappingGenerator mappingGenerator;

    private Map<String, INodeParser> parsers;

    @PostConstruct
    public void initialize() throws ParsingException {
        parsers = mappingGenerator.process("classpath:matching-configuration-dsl.yml");
    }

    @Override
    protected INodeParser<MatchingConfigurations> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        context.setRegistry(parsers);
        return parsers.get(MATCHING_CONFIGURATION_TYPE);
    }
}