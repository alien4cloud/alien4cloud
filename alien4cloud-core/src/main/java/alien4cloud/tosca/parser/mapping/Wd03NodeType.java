package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.tosca.container.model.template.DeploymentArtifact;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.RequirementDefinition;
import alien4cloud.tosca.model.AttributeDefinition;
import alien4cloud.tosca.parser.impl.advanced.InterfaceParser;
import alien4cloud.tosca.parser.impl.advanced.InterfacesParser;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.SequenceToMapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03NodeType extends Wd03InheritableToscaElement<IndexedNodeType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;
    @Resource
    private Wd03RequirementDefinition requirementDefinition;
    @Resource
    private CapabilityParser capabilityParser;
    @Resource
    private InterfaceParser interfaceParser;
    @Resource
    private Wd03DeploymentArtifactDefinition artifactDefinition;

    public Wd03NodeType() {
        super(new TypeNodeParser<IndexedNodeType>(IndexedNodeType.class, "Node type"), IndexedNodeType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(new MapParser<AttributeDefinition>(attributeDefinition.getParser(), "Attributes"), "attributes");
        quickMap(new ListParser<RequirementDefinition>(requirementDefinition.getParser(), "Requirements", "id"), "requirements");
        quickMap(new ListParser<CapabilityDefinition>(capabilityParser, "Capabilities", "id"), "capabilities");
        quickMap(new SequenceToMapParser<DeploymentArtifact>(artifactDefinition.getParser(), "Artifacts"), "artifacts");
        quickMap(new InterfacesParser(interfaceParser, "interfaces"), "interfaces");
    }
}