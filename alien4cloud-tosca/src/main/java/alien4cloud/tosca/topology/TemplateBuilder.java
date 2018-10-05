package alien4cloud.tosca.topology;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.utils.CloneUtil;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.exceptions.ConstraintFunctionalException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.AbstractInstantiableTemplate;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.PolicyTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PolicyType;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static alien4cloud.utils.AlienUtils.safe;

/**
 * Utility to create a Node Template by merging Node Type and Node Template data.
 */
@Slf4j
@Component
public class TemplateBuilder {

    /**
     * Call the template builder but ensure that a TOSCA context has been created if none exists.
     * 
     * @param dependencies The dependencies to create the tosca context.
     * @param indexedNodeType The instance of the node type.
     * @return a node template instance.
     */
    @ToscaContextual
    public NodeTemplate buildNodeTemplate(Set<CSARDependency> dependencies, NodeType indexedNodeType) {
        log.debug("Used dependencies to create a new tosca context", dependencies);
        return buildNodeTemplate(indexedNodeType);
    }

    /**
     * Build a node template. Note that a Tosca Context is required.
     *
     * @param indexedNodeType the type of the node
     * @return new constructed node template.
     */
    public static NodeTemplate buildNodeTemplate(NodeType indexedNodeType) {
        return buildNodeTemplate(indexedNodeType, null);
    }

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
     * @param nodeType the type of the node
     * @param templateToMerge the template that can be used to merge into the new node template
     * @param adaptToType This flag allow to know if we should adapt the templateToMerge node to the type.
     * @return new constructed node template.
     */
    public static NodeTemplate buildNodeTemplate(NodeType nodeType, NodeTemplate templateToMerge, boolean adaptToType) {
        // clone the type of the node to avoid impacts, this should be improved in the future
        nodeType = CloneUtil.clone(nodeType);

        NodeTemplate nodeTemplate = new NodeTemplate();
        fillAbstractInstantiableTemplate(nodeTemplate, nodeType, templateToMerge, !adaptToType);

        nodeTemplate.setCapabilities(Maps.newLinkedHashMap());
        nodeTemplate.setRequirements(Maps.newLinkedHashMap());

        fillCapabilitiesMap(nodeTemplate.getCapabilities(), nodeType.getCapabilities(), templateToMerge != null ? templateToMerge.getCapabilities() : null,
                adaptToType);
        fillRequirementsMap(nodeTemplate.getRequirements(), nodeType.getRequirements(), templateToMerge != null ? templateToMerge.getRequirements() : null,
                adaptToType);

        if (templateToMerge != null && templateToMerge.getRelationships() != null) {
            nodeTemplate.setRelationships(templateToMerge.getRelationships());
        }

        if (templateToMerge != null && templateToMerge.getTags() != null) {
            nodeTemplate.setTags(templateToMerge.getTags());
        }

        return nodeTemplate;
    }

    public static PolicyTemplate buildPolicyTemplate(PolicyType policyType) {
        policyType = CloneUtil.clone(policyType);
        PolicyTemplate policyTemplate = new PolicyTemplate();
        fillAbstractTemplate(policyTemplate, policyType, null, false);
        return policyTemplate;
    }

    public static PolicyTemplate buildPolicyTemplate(PolicyType policyType, PolicyTemplate templateToMerge, boolean adaptToType) {
        policyType = CloneUtil.clone(policyType);
        PolicyTemplate policyTemplate = new PolicyTemplate();
        fillAbstractTemplate(policyTemplate, policyType, templateToMerge, !adaptToType);
        return policyTemplate;
    }

    private static void fillAbstractTemplate(AbstractTemplate template, AbstractInheritableToscaType type, AbstractTemplate templateToMerge,
            boolean mergeUndefinedProps) {
        template.setType(type.getElementId());
        template.setProperties(Maps.newLinkedHashMap());
        fillProperties(template.getProperties(), type.getProperties(), templateToMerge != null ? templateToMerge.getProperties() : null, !mergeUndefinedProps);
    }

    private static void fillAbstractInstantiableTemplate(AbstractInstantiableTemplate template, AbstractInstantiableToscaType type,
            AbstractInstantiableTemplate templateToMerge, boolean mergeUndefinedProps) {
        fillAbstractTemplate(template, type, templateToMerge, mergeUndefinedProps);
        template.setArtifacts(Maps.newLinkedHashMap());
        fillDeploymentArtifactsMap(template.getArtifacts(), type.getArtifacts(), templateToMerge != null ? templateToMerge.getArtifacts() : null);
        // For now we just copy attributes as is.
        template.setAttributes(type.getAttributes());
        if (templateToMerge != null && templateToMerge.getInterfaces() != null) {
            // FIXME we should merge here rather than replace
            template.setInterfaces(templateToMerge.getInterfaces());
        }
    }

    private static void fillDeploymentArtifactsMap(Map<String, DeploymentArtifact> deploymentArtifacts, Map<String, DeploymentArtifact> fromTypeArtifacts,
            Map<String, DeploymentArtifact> mapToMerge) {
        if (MapUtils.isEmpty(fromTypeArtifacts)) {
            fromTypeArtifacts = Maps.newLinkedHashMap();
        }
        // We need to clone objects
        deploymentArtifacts.putAll(fromTypeArtifacts);

        for (Map.Entry<String, DeploymentArtifact> entryArtifact : safe(mapToMerge).entrySet()) {
            deploymentArtifacts.put(entryArtifact.getKey(), entryArtifact.getValue());
            DeploymentArtifact artifactFromType = deploymentArtifacts.get(entryArtifact.getKey());
            if (artifactFromType != null && StringUtils.isBlank(entryArtifact.getValue().getArtifactType())) {
                entryArtifact.getValue().setArtifactType(artifactFromType.getArtifactType());
            }
            if (artifactFromType.getArtifactRepository() == null) {
                artifactFromType.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_TOPOLOGY_REPOSITORY);
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
                // Inject all default values from the type.
                capaProperties = PropertyUtil.getDefaultPropertyValuesFromPropertyDefinitions(capabilityType.getProperties());
                // Override them with values as defined in the actual Capability Definition of the node type.
                if (capa.getProperties() != null) {
                    capaProperties.putAll(capa.getProperties());
                }
            }
            // only merge if the types are equals
            if (toAddCapa == null || (StringUtils.isNotBlank(toAddCapa.getType()) && !Objects.equals(toAddCapa.getType(), capa.getType()))) {
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
            if (toAddRequirement == null || !Objects.equals(toAddRequirement.getType(), requirement.getType())) {
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
            } else if (originalValue instanceof FunctionPropertyValue || originalValue instanceof ConcatPropertyValue) {
                properties.put(entry.getKey(), originalValue);
            } else {
                // we check the property type before accepting it
                try {
                    ConstraintPropertyService.checkPropertyConstraint(entry.getKey(), originalValue, entry.getValue());
                    properties.put(entry.getKey(), originalValue);
                } catch (ConstraintFunctionalException e) {
                    log.debug("Not able to merge property <" + entry.getKey() + "> value due to a type check exception", e);
                    if (adaptToType) {
                        AbstractPropertyValue pv = PropertyUtil.getDefaultPropertyValueFromPropertyDefinition(entry.getValue());
                        properties.put(entry.getKey(), pv);
                    }
                }
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