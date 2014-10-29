package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.parser.TypeNodeParser;

@Component
public class Wd03NodeType extends Wd03InheritableToscaElement<IndexedNodeType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;
    @Resource
    private Wd03CapabilityDefinition capabilityDefinition;
    @Resource
    private Wd03RequirementDefinition requirementDefinition;

    public Wd03NodeType() {
        super(new TypeNodeParser<IndexedNodeType>(IndexedNodeType.class, "Node type"), IndexedNodeType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(attributeDefinition.getParser(), "attributes");
        quickMap(capabilityDefinition.getParser(), "capabilities");
        quickMap(requirementDefinition.getParser(), "requirements");
    }
}