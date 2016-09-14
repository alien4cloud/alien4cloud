package alien4cloud.tosca.parser.impl.advanced;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.exception.InvalidArgumentException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;

@Component
public class ComplexPropertyValueParser implements INodeParser<AbstractPropertyValue> {

    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            return new ComplexPropertyValue(ParserUtils.parseMap((MappingNode) node));
        } else if (node instanceof SequenceNode) {
            return new ListPropertyValue(ParserUtils.parseSequence((SequenceNode) node));
        } else {
            throw new InvalidArgumentException("Do not expect other node than MappingNode or SequenceNode here " + node.getClass().getName());
        }
    }
}
