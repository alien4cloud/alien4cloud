package alien4cloud.tosca.topology;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.PropertyUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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
    public static NodeTemplate buildNodeTemplate(NodeType indexedNodeType, NodeTemplate templateToMerge) {
        return buildNodeTemplate(indexedNodeType, templateToMerge, true);
    }

    /**
     * Build a node template. Note that a Tosca Context is required.
     *
     * @param indexedNodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @param adaptToType This flag allow to know if we should adapt the templateToMerge node to the type.
     * @return new constructed node template.
     */
    public static NodeTemplate buildNodeTemplate(NodeType indexedNodeType, NodeTemplate templateToMerge, boolean adaptToType) {
        NodeTemplate nodeTemplate = new NodeTemplate();
        nodeTemplate.setType(indexedNodeType.getElementId());
        Map<String, Capability> capabilities = Maps.newLinkedHashMap();
        Map<String, Requirement> requirements = Maps.newLinkedHashMap();
        Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
        Map<String, DeploymentArtifact> deploymentArtifacts = Maps.newLinkedHashMap();

        fillDeploymentArtifactsMap(deploymentArtifacts, indexedNodeType.getArtifacts(), templateToMerge != null ? templateToMerge.getArtifacts() : null);
        fillCapabilitiesMap(capabilities, indexedNodeType.getCapabilities(), templateToMerge != null ? templateToMerge.getCapabilities() : null, adaptToType);
        fillRequirementsMap(requirements, indexedNodeType.getRequirements(), templateToMerge != null ? templateToMerge.getRequirements() : null, adaptToType);
        fillProperties(properties, indexedNodeType.getProperties(), templateToMerge != null ? templateToMerge.getProperties() : null, adaptToType);

        nodeTemplate.setCapabilities(capabilities);
        nodeTemplate.setRequirements(requirements);
        nodeTemplate.setProperties(properties);
        nodeTemplate.setAttributes(indexedNodeType.getAttributes());
        nodeTemplate.setArtifacts(deploymentArtifacts.isEmpty() ? null : deploymentArtifacts);
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

    private static void fillDeploymentArtifactsMap(Map<String, DeploymentArtifact> deploymentArtifacts, Map<String, DeploymentArtifact> fromTypeArtifacts,
            Map<String, DeploymentArtifact> mapToMerge) {
        if (MapUtils.isEmpty(fromTypeArtifacts)) {
            fromTypeArtifacts = new HashMap<>();
        }
        deploymentArtifacts.putAll(fromTypeArtifacts);
        if (mapToMerge != null) {
            for (Map.Entry<String, DeploymentArtifact> entryArtifact : mapToMerge.entrySet()) {
                deploymentArtifacts.put(entryArtifact.getKey(), entryArtifact.getValue());
                DeploymentArtifact artifactFromType = deploymentArtifacts.get(entryArtifact.getKey());
                if (artifactFromType != null && StringUtils.isBlank(entryArtifact.getValue().getArtifactType())) {
                    entryArtifact.getValue().setArtifactType(artifactFromType.getArtifactType());
                }
            }
        }
    }

    private static void fillCapabilitiesMap(Map<String, Capability> map, List<CapabilityDefinition> elements, Map<String, Capability> mapToMerge,
                                            boolean adaptToType) {
        if (elements == null) {
            return;
        }
        for (CapabilityDefinition capa : elements) {
            Capability toAddCapa = MapUtils.getObject(mapToMerge, capa.getId());
            Map<String, AbstractPropertyValue> capaProperties = null;
            CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, capa.getType());
            if (capabilityType != null && capabilityType.getProperties() != null) {
                capaProperties = PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(capabilityType.getProperties());
            }
            if (toAddCapa == null) {
                toAddCapa = new Capability();
                toAddCapa.setType(capa.getType());
                toAddCapa.setProperties(capaProperties);
            } else {
                if (StringUtils.isBlank(toAddCapa.getType())) {
                    toAddCapa.setType(capa.getType());
                }
                if (MapUtils.isNotEmpty(capaProperties)) {
                    Map<String, AbstractPropertyValue> nodeCapaProperties = safe(toAddCapa.getProperties());
                    capaProperties.putAll(nodeCapaProperties);
                    toAddCapa.setProperties(capaProperties);
                }
            }

            Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
            fillProperties(properties, capabilityType != null ? capabilityType.getProperties() : null, toAddCapa.getProperties(), adaptToType);
            toAddCapa.setProperties(properties);
            map.put(capa.getId(), toAddCapa);
        }
    }

    private static void fillRequirementsMap(Map<String, Requirement> map, List<RequirementDefinition> elements, Map<String, Requirement> mapToMerge,
                                            boolean adaptToType) {
        if (elements == null) {
            return;
        }
        for (RequirementDefinition requirement : elements) {
            Requirement toAddRequirement = MapUtils.getObject(mapToMerge, requirement.getId());
            // the type of a requirement is a capability type in TOSCA as they match each other.
            CapabilityType requirementType = ToscaContext.get(CapabilityType.class, requirement.getType());
            if (toAddRequirement == null) {
                toAddRequirement = new Requirement();
                toAddRequirement.setType(requirement.getType());
            }

            Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
            fillProperties(properties, requirementType != null ? requirementType.getProperties() : null, toAddRequirement.getProperties(), adaptToType);
            toAddRequirement.setProperties(properties);
            map.put(requirement.getId(), toAddRequirement);
        }
    }

    public static void fillProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertiesDefinitions,
            Map<String, AbstractPropertyValue> originalProperties) {
        fillProperties(properties, propertiesDefinitions, originalProperties, true);
    }

    @SneakyThrows
    public static void fillProperties(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertiesDefinitions,
            Map<String, AbstractPropertyValue> originalProperties, boolean adaptToType) {
        if (propertiesDefinitions == null || properties == null) {
            return;
        }
        for (Map.Entry<String, PropertyDefinition> entry : propertiesDefinitions.entrySet()) {
            AbstractPropertyValue originalValue = MapUtils.getObject(originalProperties, entry.getKey());
            if (originalValue == null) {
                AbstractPropertyValue pv = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(entry.getValue());
                properties.put(entry.getKey(), pv);
            } else {
                // FIXME we should check the property type before accepting it
                properties.put(entry.getKey(), originalValue);
            }
        }
        if (!adaptToType) {
            // we have to add the properties defined on the given originalProperties even if not defined on the type (could be for later validations).
            // maybe we could put validations here actually.
            for (Map.Entry<String, AbstractPropertyValue> originalProperty : safe(originalProperties).entrySet()) {
                if (!properties.containsKey(originalProperty.getKey())) {
                    properties.put(originalProperty.getKey(), originalProperty.getValue());
                }
            }
        }
    }
}