package alien4cloud.tosca.topology;

import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.CapabilityDefinition;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;

import com.google.common.collect.Maps;

/**
 * Utility to create a Node Template by merging Node Type and Node Template data.
 */
@Slf4j
public class NodeTemplateBuilder {

    /**
     * Build a node template. Note that a Tosca Context is required.
     *
     * @param indexedNodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @return new constructed node template.
     */
    public static NodeTemplate buildNodeTemplate(IndexedNodeType indexedNodeType, NodeTemplate templateToMerge) {
        NodeTemplate nodeTemplate = new NodeTemplate();
        nodeTemplate.setType(indexedNodeType.getElementId());
        Map<String, Capability> capabilities = Maps.newLinkedHashMap();
        Map<String, Requirement> requirements = Maps.newLinkedHashMap();
        Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
        Map<String, DeploymentArtifact> deploymentArtifacts = null;
        Map<String, DeploymentArtifact> deploymentArtifactsToMerge = templateToMerge != null ? templateToMerge.getArtifacts() : null;
        if (deploymentArtifactsToMerge != null) {
            if (indexedNodeType.getArtifacts() != null) {
                deploymentArtifacts = Maps.newLinkedHashMap(indexedNodeType.getArtifacts());
                for (Map.Entry<String, DeploymentArtifact> entryArtifact : deploymentArtifactsToMerge.entrySet()) {
                    DeploymentArtifact existingArtifact = entryArtifact.getValue();
                    if (deploymentArtifacts.containsKey(entryArtifact.getKey())) {
                        deploymentArtifacts.put(entryArtifact.getKey(), existingArtifact);
                    }
                }
            }
        } else if (indexedNodeType.getArtifacts() != null) {
            deploymentArtifacts = Maps.newLinkedHashMap(indexedNodeType.getArtifacts());
        }
        fillCapabilitiesMap(capabilities, indexedNodeType.getCapabilities(), templateToMerge != null ? templateToMerge.getCapabilities() : null);
        fillRequirementsMap(requirements, indexedNodeType.getRequirements(), templateToMerge != null ? templateToMerge.getRequirements() : null);
        fillProperties(properties, indexedNodeType.getProperties(), templateToMerge != null ? templateToMerge.getProperties() : null);
        nodeTemplate.setCapabilities(capabilities);
        nodeTemplate.setRequirements(requirements);
        nodeTemplate.setProperties(properties);
        nodeTemplate.setAttributes(indexedNodeType.getAttributes());
        nodeTemplate.setArtifacts(deploymentArtifacts);
        if (templateToMerge != null) {
            if (templateToMerge.getInterfaces() != null) {
                nodeTemplate.setInterfaces(templateToMerge.getInterfaces());
            }
            if (templateToMerge.getRelationships() != null) {
                nodeTemplate.setRelationships(templateToMerge.getRelationships());
            }
        }
        return nodeTemplate;
    }

    private static void fillCapabilitiesMap(Map<String, Capability> map, List<CapabilityDefinition> elements, Map<String, Capability> mapToMerge) {
        if (elements == null) {
            return;
        }
        for (CapabilityDefinition capa : elements) {
            Capability toAddCapa = MapUtils.getObject(mapToMerge, capa.getId());
            if (toAddCapa == null) {
                toAddCapa = new Capability();
                toAddCapa.setType(capa.getType());
                IndexedCapabilityType indexedCapa = ToscaContext.get(IndexedCapabilityType.class, capa.getType());
                if (indexedCapa != null && indexedCapa.getProperties() != null) {
                    toAddCapa.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedCapa.getProperties()));
                }
            }
            map.put(capa.getId(), toAddCapa);
        }
    }

    private static void fillRequirementsMap(Map<String, Requirement> map, List<RequirementDefinition> elements, Map<String, Requirement> mapToMerge) {
        if (elements == null) {
            return;
        }
        for (RequirementDefinition requirement : elements) {
            Requirement toAddRequirement = MapUtils.getObject(mapToMerge, requirement.getId());
            if (toAddRequirement == null) {
                toAddRequirement = new Requirement();
                toAddRequirement.setType(requirement.getType());
                IndexedCapabilityType indexedReq = ToscaContext.get(IndexedCapabilityType.class, requirement.getType());
                if (indexedReq != null && indexedReq.getProperties() != null) {
                    toAddRequirement.setProperties(PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(indexedReq.getProperties()));
                }
            }
            map.put(requirement.getId(), toAddRequirement);
        }
    }

    @SneakyThrows
    public static void fillProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertiesDefinitions,
            Map<String, AbstractPropertyValue> map) {
        if (propertiesDefinitions == null || properties == null) {
            return;
        }
        for (Map.Entry<String, PropertyDefinition> entry : propertiesDefinitions.entrySet()) {
            AbstractPropertyValue existingValue = MapUtils.getObject(map, entry.getKey());
            if (existingValue == null) {
                AbstractPropertyValue pv = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(entry.getValue());
                properties.put(entry.getKey(), pv);
            } else {
                properties.put(entry.getKey(), existingValue);
            }
        }
    }
}
