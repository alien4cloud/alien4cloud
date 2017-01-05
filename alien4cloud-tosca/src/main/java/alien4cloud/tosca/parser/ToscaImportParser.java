package alien4cloud.tosca.parser;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.alien4cloud.tosca.model.CsarDependenciesBean;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;

/**
 * Parse a TOSCA Template to retrieve only some meta and dependencies (imports section)
 */
@Component
public class ToscaImportParser extends YamlParser<CsarDependenciesBean> {
    private static final String DEFINITION_TYPE = "definition";

    @Resource
    private MappingGenerator mappingGenerator;

    private Map<String, INodeParser> parsers;

    @PostConstruct
    public void initialize() throws ParsingException {
        parsers = mappingGenerator.process("classpath:tosca-simple-profile-import-mapping.yml");
    }

    @Override
    protected INodeParser<CsarDependenciesBean> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        context.setRegistry(parsers);
        return parsers.get(DEFINITION_TYPE);
    }

    @Override
    protected void postParsing(CsarDependenciesBean result) {
        // Nothing to do here for now
        return;
    }
}