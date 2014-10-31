package alien4cloud.tosca.parser.mapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.TypeReferenceParserFactory;
import alien4cloud.tosca.parser.impl.TypeReferenceParser;

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
    public CapabilityDefinition parse(Node node, ParsingContext context) {
        if (node instanceof ScalarNode) {
            String capabilityType = typeReferenceParser.parse(node, context);
            if (capabilityType == null) {
                return null;
            }
            CapabilityDefinition definition = new CapabilityDefinition();
            definition.setType(capabilityType);
            return definition;
        }

        return capabilityDefinition.getParser().parse(node, context);
    }
}