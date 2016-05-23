package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.tosca.parser.DefferedParsingValueExecutor;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.base.ReferencedParser;
import alien4cloud.tosca.parser.mapping.DefaultParser;

@Component
public class CapabilityDefinitionParser extends DefaultParser<CapabilityDefinition> {
    @Resource
    private ReferencedCapabilityTypeParser referencedCapabilityTypeParser;
    private final ReferencedParser<CapabilityDefinition> capabilityDefinitionParser;

    public CapabilityDefinitionParser() {
        this.capabilityDefinitionParser = new ReferencedParser("capability_definition_detailed");
    }

    @Override
    public CapabilityDefinition parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            CapabilityDefinition definition = new CapabilityDefinition();
            BeanWrapper instanceWrapper = new BeanWrapperImpl(definition);
            context.addDeferredParser(new DefferedParsingValueExecutor(null, instanceWrapper, context,
                    new MappingTarget("type", referencedCapabilityTypeParser), node));
            return definition;
        }

        return capabilityDefinitionParser.parse(node, context);
    }
}