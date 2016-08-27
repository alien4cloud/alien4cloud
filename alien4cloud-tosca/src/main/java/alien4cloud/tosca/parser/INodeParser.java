package alien4cloud.tosca.parser;

import org.yaml.snakeyaml.nodes.Node;

/**
 * Interface for a node parser.
 *
 * @param <T> The type of returned by the parser.
 */
public interface INodeParser<T> {
    /**
     * Parse a yaml node.
     * 
     * @param node The node to parse.
     * @param context The parsing context that contains the root object as well as errors and deferred parsers to be executed after the primary parsing is done.
     * @return An instance of T based on the node parsing or null if the parsing failed.
     */
    T parse(Node node, ParsingContextExecution context);
}