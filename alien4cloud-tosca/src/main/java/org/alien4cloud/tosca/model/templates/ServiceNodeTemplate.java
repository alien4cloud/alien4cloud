package org.alien4cloud.tosca.model.templates;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import java.util.Map;

/**
 * A {@link NodeTemplate} that binds to an exiting running service.
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceNodeTemplate extends NodeTemplate {

    private String serviceResourceId;

    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private Map<String, String> attributeValues;

    public ServiceNodeTemplate(NodeInstance nodeInstance) {
        this(nodeInstance.getNodeTemplate(), nodeInstance.getAttributeValues());
    }

    public ServiceNodeTemplate(NodeTemplate nodeTemplate, Map<String, String> attributeValues) {
        super(nodeTemplate.getType(), nodeTemplate.getProperties(), nodeTemplate.getAttributes(), nodeTemplate.getRelationships(),
                nodeTemplate.getRequirements(), nodeTemplate.getCapabilities(), nodeTemplate.getInterfaces(), nodeTemplate.getArtifacts());
        this.attributeValues = attributeValues;
    }

    public ServiceNodeTemplate(String type, Map<String, AbstractPropertyValue> properties, Map<String, IValue> attributes,
            Map<String, RelationshipTemplate> relationships, Map<String, Requirement> requirements, Map<String, Capability> capabilities,
            Map<String, Interface> interfaces, Map<String, DeploymentArtifact> artifacts) {
        super(type, properties, attributes, relationships, requirements, capabilities, interfaces, artifacts);
    }
}
