package alien4cloud.tosca.parser;

import java.util.List;

import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.tosca.parser.impl.ErrorCode;

public final class ToscaParsingUtil {
    private ToscaParsingUtil() {
    }

    /**
     * Get a string value.
     * 
     * @param keyNode The node that represents the key of the value node to parse.
     * @param valueNode The value node.
     * @param parsingErrors A list of errors in which to add an error if the value is not a valid yaml string.
     * @return The value of the string. In case of an error null is returned. Null return however is not sufficient to know that an error occured. In case of an
     *         error a new {@link ParsingError} is added to the parsingErrors list given as a parameter.
     */
    public static String getStringValue(ScalarNode keyNode, Node valueNode, List<ParsingError> parsingErrors) {
        if (valueNode instanceof ScalarNode) {
            ScalarNode scalarNode = (ScalarNode) valueNode;
            return scalarNode.getValue();
        }
        parsingErrors.add(new ParsingError(ErrorCode.SYNTAX_ERROR, "Error while parsing field " + keyNode.getValue(), keyNode.getStartMark(),
                "Expected a scalar type.", valueNode.getStartMark(), "scalar"));
        return null;
    }
}