package alien4cloud.tosca.parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Path;

import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.parser.ParserImpl;
import org.yaml.snakeyaml.reader.StreamReader;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * Parser to process Yaml files.
 * 
 * @author luc boutier
 *
 * @param <T> The object instance in which to parse the object.
 */
public abstract class YamlParser<T> {
    /**
     * Parse a yaml file.
     * 
     * @param templatePath Path of the yaml file.
     * @throws FileNotFoundException In case the definition file cannot be found.
     * @throws ParsingException In case there is a blocking issue while parsing the definition.
     */
    public ParsingResult<T> parseFile(Path templatePath) throws ParsingException {
        StreamReader sreader;
        try {
            sreader = new StreamReader(new UnicodeReader(new FileInputStream(templatePath.toFile())));
        } catch (FileNotFoundException e1) {
            throw new ParsingException(templatePath.getFileName().toString(), new ParsingError("File not found in archive.", null, null, null, null));
        }
        Composer composer = new Composer(new ParserImpl(sreader), new Resolver());
        Node rootNode = null;
        try {
            rootNode = composer.getSingleNode();
            if (rootNode == null) {
                throw new ParsingException(templatePath.getFileName().toString(), new ParsingError("Empty file.", new Mark("root", 0, 0, 0, null, 0),
                        "No yaml content found in file.", new Mark("root", 0, 0, 0, null, 0), null));
            }
        } catch (MarkedYAMLException exception) {
            throw new ParsingException(templatePath.getFileName().toString(), new ParsingError(exception));
        }

        if (rootNode instanceof MappingNode) {
            try {
                return doParsing(templatePath.getFileName().toString(), (MappingNode) rootNode);
            } catch (ParsingException e) {
                throw new ParsingException(templatePath.getFileName().toString(), e.getParsingErrors());
            }
        } else {
            throw new ParsingException(templatePath.getFileName().toString(), new ParsingError("File is not a valid tosca definition file.", new Mark("root",
                    0, 0, 0, null, 0), "The provided yaml file doesn't follow the Top-level key definitions of a valid TOSCA Simple profile file.", new Mark(
                    "root", 0, 0, 0, null, 0), null));
        }
    }

    private ParsingResult<T> doParsing(String fileName, MappingNode rootNode) throws ParsingException {
        ParsingContextExecution context = new ParsingContextExecution(fileName);

        INodeParser<T> nodeParser = getParser(rootNode, context);

        // let's start the parsing using the version related parsers
        T archiveRoot = nodeParser.parse(rootNode, context);

        // process deferred parsing
        for (Runnable defferedParser : context.getDefferedParsers()) {
            defferedParser.run();
        }

        return new ParsingResult<T>(archiveRoot, context.getParsingContext());
    }

    /**
     * Allow to find the parser to use based on the root node.
     * 
     * @param rootNode The root node from which to get a parser implementation.
     * @param context The parsing context.
     * @return The parser to use.
     */
    protected abstract INodeParser<T> getParser(MappingNode rootNode, ParsingContextExecution context) throws ParsingException;
}
