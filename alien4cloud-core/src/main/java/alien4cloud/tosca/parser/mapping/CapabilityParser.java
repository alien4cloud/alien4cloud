package alien4cloud.tosca.parser.mapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.parser.DefferedParsingValueExecutor;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParser;
import alien4cloud.tosca.parser.impl.advanced.TypeReferenceParserFactory;

@Component
public class CapabilityParser implements INodeParser<CapabilityDefinition> {
    @Resource
    private TypeReferenceParserFactory typeReferenceParserFactory;
    private TypeReferenceParser typeReferenceParser;
    @Resource
    private Wd03CapabilityDefinition capabilityDefinition;

    @PostConstruct
    public void initialize() {
        typeReferenceParser = typeReferenceParserFactory.getTypeReferenceParser(IndexedCapabilityType.class, IndexedNodeType.class);
    }

    @Override
    public boolean isDeffered() {
        return false;
    }

    @Override
    public CapabilityDefinition parse(Node node, ParsingContextExecution context) {
        if (node instanceof ScalarNode) {
            CapabilityDefinition definition = new CapabilityDefinition();
            BeanWrapper instanceWrapper = new BeanWrapperImpl(definition);
            context.getDefferedParsers().add(
                    new DefferedParsingValueExecutor(null, instanceWrapper, context, new MappingTarget("type", typeReferenceParser), node));
            return definition;
        }

        return capabilityDefinition.getParser().parse(node, context);
    }
}