package alien4cloud.tosca.parser.impl.advanced;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.mapping.DefaultParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

@Component
@Slf4j
public class AbstractPropertyValueParser extends DefaultParser<AbstractPropertyValue> {

    @Override
    public AbstractPropertyValue parse(Node node, ParsingContextExecution context) {
        String parserName = node instanceof ScalarNode ? "scalar_property_value" : "complex_property_value";
        INodeParser<AbstractPropertyValue> paser = context.getRegistry().get(parserName);
        return paser.parse(node, context);
    }

}