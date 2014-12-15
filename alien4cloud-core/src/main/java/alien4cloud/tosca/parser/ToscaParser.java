package alien4cloud.tosca.parser;

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

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.mapping.generator.MappingGenerator;

import com.google.common.collect.Maps;

/**
 * Main entry point for TOSCA template parsing.
 */
@Component
public class ToscaParser extends YamlParser<ArchiveRoot> {
    private static final String DEFINITION_TYPE = "definition";
    private Map<String, Map<String, INodeParser>> parserRegistriesByVersion = Maps.newHashMap();

    @Resource
    private MappingGenerator mappingGenerator;

    @PostConstruct
    public void initialize() throws ParsingException {
        // initialize type registry for working draft 3.
        Map<String, INodeParser> registry = mappingGenerator.process("classpath:tosca-simple-profile-wd03-mapping.yml");
        parserRegistriesByVersion.put("tosca_simple_yaml_1_0_0_wd03", registry);
    }

    @Override
    protected INodeParser<ArchiveRoot> getParser(Node rootNode, ParsingContextExecution context) throws ParsingException {
        if (rootNode instanceof MappingNode) {
            // try to find the tosca version
            DefinitionVersionInfo definitionVersionInfo = getToscaDefinitionVersion(((MappingNode) rootNode).getValue(), context.getParsingErrors());
            // call the parser for the given tosca version
            Map<String, INodeParser> registry = parserRegistriesByVersion.get(definitionVersionInfo.definitionVersion);
            if (registry == null) {
                throw new ParsingException(context.getFileName(), new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.MISSING_TOSCA_VERSION,
                        "Definition version is not supported", definitionVersionInfo.definitionVersionTuple.getKeyNode().getStartMark(),
                        "Version is not supported by Alien4Cloud", definitionVersionInfo.definitionVersionTuple.getValueNode().getStartMark(),
                        definitionVersionInfo.definitionVersion));
            }
            context.setRegistry(registry);
            return registry.get(DEFINITION_TYPE);
        } else {
            throw new ParsingException(null, new ParsingError(ErrorCode.SYNTAX_ERROR, "File is not a valid tosca definition file.", new Mark("root", 0, 0, 0,
                    null, 0), "The provided yaml file doesn't follow the Top-level key definitions of a valid TOSCA Simple profile file.", new Mark("root", 0,
                    0, 0, null, 0), "TOSCA Definitions"));
        }
    }

    private DefinitionVersionInfo getToscaDefinitionVersion(List<NodeTuple> topLevelNodes, List<ParsingError> parsingErrors) throws ParsingException {
        for (NodeTuple node : topLevelNodes) {
            Node key = node.getKeyNode();
            if (key instanceof ScalarNode) {
                ScalarNode scalarKey = (ScalarNode) key;
                if (scalarKey.getValue().equals("tosca_definitions_version")) {
                    return new DefinitionVersionInfo(ToscaParsingUtil.getStringValue(scalarKey, node.getValueNode(), parsingErrors), node);
                }
            }
        }
        throw new ParsingException(null, new ParsingError(ErrorCode.MISSING_TOSCA_VERSION, "File is not a valid tosca definition file.", new Mark("root", 0, 0,
                0, null, 0), "Unable to find the mandatory tosca_definitions_version.", new Mark("root", 0, 0, 0, null, 0), null));
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