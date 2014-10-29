package alien4cloud.tosca.parser;

import org.yaml.snakeyaml.nodes.NodeTuple;

public interface INodeTupleParser<T> extends IParser {
    /**
     * Parse a yaml node tuple.
     * 
     * @param node The node tuple to parse.
     * @param context The parsing context that contains the root object as well as errors and deffered parsers to be executed after the primary parsing is done.
     * @return An instance of T based on the node parsing or null if the parsing failed.
     */
    T parse(NodeTuple node, ParsingContext context);
}
