package alien4cloud.tosca.parser;

import lombok.AllArgsConstructor;

import org.yaml.snakeyaml.nodes.MappingNode;

@AllArgsConstructor
public class YamlSimpleParser<T> extends YamlParser<T> {
    private INodeParser<T> nodeParser;

    @Override
    protected INodeParser<T> getParser(MappingNode rootNode, ParsingContext context) throws ParsingException {
        return nodeParser;
    }
}
