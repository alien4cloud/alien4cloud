package alien4cloud.tosca.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.resolver.Resolver;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.mapping.Wd03ArchiveRoot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Main entry point for TOSCA template parsing.
 */
@Component
public class ToscaParser {
    @Resource
    public Wd03ArchiveRoot wd03ArchiveRoot;

    private Map<String, INodeParser<ArchiveRoot>> nodeParserRegistry = Maps.newHashMap();

    @PostConstruct
    public void initialize() {
        nodeParserRegistry.put(Wd03ArchiveRoot.VERSION, wd03ArchiveRoot.getParser());
    }

    /**
     * Parse a tosca definition file.
     * 
     * @param templatePath Path of the TOSCA definition file.
     * @throws FileNotFoundException In case the definition file cannot be found.
     * @throws ToscaParsingException In case there is a blocking issue while parsing the definition.
     */
    public ParsingResult parseDefinition(Path templatePath) throws FileNotFoundException, ToscaParsingException {
        return parseDefinition(templatePath, null);
    }

    /**
     * Parse a tosca definition file.
     * 
     * @param templatePath Path of the TOSCA definition file.
     * @param templateName The TOSCA template name as specified during upload to provide an override name for the TOSCA 'template_name'. If null the
     *            'template_name' that is optional in TOSCA will be required for Alien4Cloud.
     * @throws FileNotFoundException In case the definition file cannot be found.
     * @throws ToscaParsingException In case there is a blocking issue while parsing the definition.
     */
    public ParsingResult parseDefinition(Path templatePath, String templateName) throws FileNotFoundException, ToscaParsingException {
        StreamReader sreader = new StreamReader(new UnicodeReader(new FileInputStream(templatePath.toFile())));
        Composer composer = new Composer(new ParserImpl(sreader), new Resolver());
        Node rootNode = null;
        try {
            rootNode = composer.getSingleNode();
            if (rootNode == null) {
                throw new ToscaParsingException(new ToscaParsingError(templatePath.getFileName().toString(), "Empty file.", new Mark("root", 0, 0, 0, null, 0),
                        "No yaml content found in file.", new Mark("root", 0, 0, 0, null, 0), null));
            }
        } catch (MarkedYAMLException exception) {
            throw new ToscaParsingException(new ToscaParsingError(templatePath.getFileName().toString(), exception));
        }

        if (rootNode instanceof MappingNode) {
            return doParsing((MappingNode) rootNode);
        } else {
            throw new ToscaParsingException(new ToscaParsingError(templatePath.getFileName().toString(), "File is not a valid tosca definition file.",
                    new Mark("root", 0, 0, 0, null, 0),
                    "The provided yaml file doesn't follow the Top-level key definitions of a valid TOSCA Simple profile file.", new Mark("root", 0, 0, 0,
                            null, 0), null));
        }
    }

    private ParsingResult doParsing(MappingNode rootNode) throws ToscaParsingException {
        List<ToscaParsingError> parsingErrors = Lists.newArrayList();
        List<Runnable> defferedParsers = Lists.newArrayList();

        // try to find the tosca version
        DefinitionVersionInfo definitionVersionInfo = getToscaDefinitionVersion(rootNode.getValue(), parsingErrors);
        // call the parser for the given tosca version
        INodeParser<ArchiveRoot> nodeParser = nodeParserRegistry.get(definitionVersionInfo.definitionVersion);
        if (nodeParser == null) {
            throw new ToscaParsingException(new ToscaParsingError(null, "Definition version is not supported", definitionVersionInfo.definitionVersionTuple
                    .getKeyNode().getStartMark(), "Version is not supported by Alien4Cloud", definitionVersionInfo.definitionVersionTuple.getValueNode()
                    .getStartMark(), definitionVersionInfo.definitionVersion));
        }

        ParsingContext context = new ParsingContext(parsingErrors, defferedParsers);

        // let's start the parsing using the version related parsers
        ArchiveRoot archiveRoot = nodeParser.parse(rootNode, context);

        // process deferred parsing
        for (Runnable defferedParser : defferedParsers) {
            defferedParser.run();
        }

        return new ParsingResult(archiveRoot, parsingErrors);
    }

    private DefinitionVersionInfo getToscaDefinitionVersion(List<NodeTuple> topLevelNodes, List<ToscaParsingError> parsingErrors) throws ToscaParsingException {
        for (NodeTuple node : topLevelNodes) {
            Node key = node.getKeyNode();
            if (key instanceof ScalarNode) {
                ScalarNode scalarKey = (ScalarNode) key;
                if (scalarKey.getValue().equals("tosca_definitions_version")) {
                    return new DefinitionVersionInfo(ToscaParsingUtil.getStringValue(scalarKey, node.getValueNode(), parsingErrors), node);
                }
            }
        }
        throw new ToscaParsingException(new ToscaParsingError(null, "File is not a valid tosca definition file.", new Mark("root", 0, 0, 0, null, 0),
                "Unable to find the mandatory tosca_definitions_version.", new Mark("root", 0, 0, 0, null, 0), null));
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