package alien4cloud.tosca.parser.impl;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContext;

/**
 * Very simple scalar parser that just returns the value as string.
 */
@Component
public class ScalarParser implements INodeParser<String> {
    @Override
    public boolean isDeffered() {
        return false;
    }

    @Override
    public String parse(Node node, ParsingContext context) {
        return ParserUtils.getScalar(node, context.getParsingErrors());
    }
}