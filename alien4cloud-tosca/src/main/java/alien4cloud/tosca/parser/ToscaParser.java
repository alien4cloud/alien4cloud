package alien4cloud.tosca.parser;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.google.common.collect.Maps;

import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;
import alien4cloud.tosca.parser.postprocess.ArchiveRootPostProcessor;

/**
 * Main entry point for TOSCA template parsing.
 */
@Component
public class ToscaParser extends YamlParser<ArchiveRoot> {
    private static final String DEFINITION_TYPE = "definition";
    private Map<String, Map<String, INodeParser>> parserRegistriesByVersion = Maps.newHashMap();

    @Resource
    private MappingGenerator mappingGenerator;

    @Resource
    private ArchiveRootPostProcessor archiveRootPostProcessor;

    @PostConstruct
    public void initialize() throws ParsingException {
        // initialize type registry for working draft 3.
        Map<String, INodeParser> registry = mappingGenerator.process("classpath:tosca-simple-profile-wd03-mapping.yml");
        parserRegistriesByVersion.put("tosca_simple_yaml_1_0_0_wd03", registry);
        registry = mappingGenerator.process("classpath:alien-dsl-1.1.0-mapping.yml");
        parserRegistriesByVersion.put("alien_dsl_1_1_0", registry);
        registry = mappingGenerator.process("classpath:alien-dsl-1.2.0-mapping.yml");
        parserRegistriesByVersion.put("alien_dsl_1_2_0", registry);
        registry = mappingGenerator.process("classpath:alien-dsl-1.3.0-mapping.yml");
        parserRegistriesByVersion.put("alien_dsl_1_3_0", registry);
        // experimental
        registry = mappingGenerator.process("classpath:tosca_simple_yaml_1_0.yml");
        parserRegistriesByVersion.put("tosca_simple_yaml_1_0", registry);
        parserRegistriesByVersion.put("http://docs.oasis-open.org/tosca/ns/simple/yaml/1.0", registry);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(String filePath, String fileName, InputStream yamlStream, ArchiveRoot instance) throws ParsingException {
        return super.parseFile(filePath, fileName, yamlStream, instance);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(Path yamlPath) throws ParsingException {
        return super.parseFile(yamlPath);
    }

    @Override
    @ToscaContextual
    public ParsingResult<ArchiveRoot> parseFile(Path yamlPath, ArchiveRoot instance) throws ParsingException {
        return super.parseFile(yamlPath, instance);
    }

    @Override
    protected void postParsing(ArchiveRoot result) {
        // Perform post processing model manipulation and validations.
        archiveRootPostProcessor.process(result);
    }

    @Override
    protected INodeParser<ArchiveRoot> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        if (rootNode instanceof MappingNode) {
            // try to find the tosca version
            DefinitionVersionInfo definitionVersionInfo = getToscaDefinitionVersion(((MappingNode) rootNode).getValue(), context);
            // call the parser for the given tosca version
            Map<String, INodeParser> registry = parserRegistriesByVersion.get(definitionVersionInfo.definitionVersion);
            if (registry == null) {
                throw new ParsingException(context.getFileName(),
                        new ParsingError(ErrorCode.UNKNOWN_TOSCA_VERSION, "Definition version is not supported",
                                definitionVersionInfo.definitionVersionTuple.getKeyNode().getStartMark(), "Version is not supported by Alien4Cloud",
                                definitionVersionInfo.definitionVersionTuple.getValueNode().getStartMark(), definitionVersionInfo.definitionVersion));
            }

            context.setRegistry(registry);
            context.setDefinitionVersion(definitionVersionInfo.definitionVersion);
            return registry.get(DEFINITION_TYPE);
        } else {
            throw new ParsingException(null,
                    new ParsingError(ErrorCode.SYNTAX_ERROR, "File is not a valid tosca definition file.", new Mark("root", 0, 0, 0, null, 0),
                            "The provided yaml file doesn't follow the Top-level key definitions of a valid TOSCA Simple profile file.",
                            new Mark("root", 0, 0, 0, null, 0), "TOSCA Definitions"));
        }
    }

    private DefinitionVersionInfo getToscaDefinitionVersion(List<NodeTuple> topLevelNodes, ParsingContextExecution context) throws ParsingException {
        boolean first = true;
        for (NodeTuple node : topLevelNodes) {
            Node key = node.getKeyNode();
            if (key instanceof ScalarNode) {
                ScalarNode scalarKey = (ScalarNode) key;
                if (scalarKey.getValue().equals("tosca_definitions_version")) {
                    if (!first) {
                        // TOSCA definition version must be the first yaml element
                        context.getParsingErrors()
                                .add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.TOSCA_VERSION_NOT_FIRST,
                                        "File is not a valid tosca definition file.", node.getKeyNode().getStartMark(),
                                        "tosca_definitions_version must be the first element of the document.", node.getValueNode().getEndMark(), null));
                    }
                    return new DefinitionVersionInfo(ParserUtils.getScalar(node.getValueNode(), context), node);
                }
            }
            first = false;
        }
        throw new ParsingException(null, new ParsingError(ErrorCode.MISSING_TOSCA_VERSION, "File is not a valid tosca definition file.",
                new Mark("root", 0, 0, 0, null, 0), "Unable to find the mandatory tosca_definitions_version.", new Mark("root", 0, 0, 0, null, 0), null));
    }

    private class DefinitionVersionInfo {
        private final String definitionVersion;
        private final NodeTuple definitionVersionTuple;

        public DefinitionVersionInfo(String definitionVersion, NodeTuple definitionVersionTuple) {
            this.definitionVersion = definitionVersion;
            this.definitionVersionTuple = definitionVersionTuple;
        }
    }
}