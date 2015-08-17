package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.ComplexPropertyValue;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class ComplexPropertyValueParser extends DefaultParser<ComplexPropertyValue> {

    @Override
    public ComplexPropertyValue parse(Node node, ParsingContextExecution context) {
        return new ComplexPropertyValue(ParserUtils.parseMap((MappingNode) node));
    }
}
