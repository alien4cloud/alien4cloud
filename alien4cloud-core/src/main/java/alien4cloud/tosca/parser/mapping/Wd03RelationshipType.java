package alien4cloud.tosca.parser.mapping;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import alien4cloud.component.model.IndexedRelationshipType;
import alien4cloud.tosca.container.model.template.DeploymentArtifact;
import alien4cloud.tosca.model.AttributeDefinition;
import alien4cloud.tosca.parser.impl.advanced.InterfaceParser;
import alien4cloud.tosca.parser.impl.advanced.InterfacesParser;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.SequenceToMapParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

@Component
public class Wd03RelationshipType extends Wd03InheritableToscaElement<IndexedRelationshipType> {
    @Resource
    private Wd03AttributeDefinition attributeDefinition;
    @Resource
    private InterfaceParser interfaceParser;
    @Resource
    private Wd03DeploymentArtifactDefinition artifactDefinition;

    public Wd03RelationshipType() {
        super(new TypeNodeParser<IndexedRelationshipType>(IndexedRelationshipType.class, "Relationship type"), IndexedRelationshipType.class);
    }

    @Override
    public void initMapping() {
        super.initMapping();
        quickMap(new MapParser<AttributeDefinition>(attributeDefinition.getParser(), "Attributes"), "attributes");
        quickMap(new InterfacesParser(interfaceParser, "interfaces"), "interfaces");
        quickMap(new SequenceToMapParser<DeploymentArtifact>(artifactDefinition.getParser(), "Artifacts"), "artifacts");
        quickMap(new ListParser<String>(getScalarParser(), "valid targets"), "validTargets");
        quickMap(new ListParser<String>(getScalarParser(), "valid sources"), "validSources");
    }
}