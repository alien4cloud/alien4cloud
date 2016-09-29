package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;

@Component
public class CapabilityDefinitionParser implements INodeParser<CapabilityDefinition> {
    @Resource
    private BaseParserFactory baseParserFactory;

    private ReferencedParser<CapabilityDefinition> capabilityDefinitionParser;

    @PostConstruct
    public void init() {
        this.capabilityDefinitionParser = baseParserFactory.getReferencedParser("capability_definition_detailed");
    }

    @Override
    public CapabilityDefinition parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            CapabilityDefinition definition = new CapabilityDefinition();
            definition.setType(((ScalarNode) node).getValue());
            return definition;
        }

        return capabilityDefinitionParser.parse(node, context);
    }
}