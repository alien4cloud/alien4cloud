package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

import java.util.Map;

@Slf4j
@Component
public class ComplexPropertySubValueParser implements INodeParser<Map<String,Object>> {

    @Override
    public Map<String,Object> parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            Map<String,Object> result = ParserUtils.parseComplexMap((MappingNode) node,context);
            return result;
        } else {
            throw new InvalidArgumentException("Do not expect other node than MappingNode or SequenceNode here " + node.getClass().getName());
        }
    }
}
