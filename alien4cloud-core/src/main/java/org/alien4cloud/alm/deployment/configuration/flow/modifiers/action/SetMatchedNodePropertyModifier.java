package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import static alien4cloud.utils.AlienUtils.safe;

import java.beans.IntrospectionException;
import java.util.Map;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodeCapabilitiesPropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import alien4cloud.utils.services.PropertyService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

/**
 * This modifier is injected when the deployment cycle is run in the context of a deployment user update to the properties of a matched node.
 *
 * It injects the custom user defined property in the node if valid.
 */
@AllArgsConstructor
public class SetMatchedNodePropertyModifier implements ITopologyModifier {
    private PropertyService propertyService;
    private String nodeTemplateId;
    private Optional<String> optionalCapabilityName;
    private String propertyName;
    private Object propertyValue;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigAutoSelectModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();

        NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeTemplateId);

        if (nodeTemplate == null) {
            throw new NotFoundException("Topology <" + topology.getId() + "> does not contains any node with id <" + nodeTemplateId + ">");
        }
        String substitutionId = lastUserSubstitutions.get(nodeTemplateId);
        if (substitutionId == null) {
            throw new NotFoundException("The node <" + nodeTemplateId + "> from topology <" + topology.getId() + "> is not matched.");
        }

        Map<String, LocationResourceTemplate> allAvailableResourceTemplate = (Map<String, LocationResourceTemplate>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP);

        LocationResourceTemplate locationResourceTemplate = allAvailableResourceTemplate.get(substitutionId);

        try {
            if (optionalCapabilityName.isPresent()) {
                setNodeCapabilityProperty(context, locationResourceTemplate, nodeTemplate, optionalCapabilityName.get(), matchingConfiguration);
            } else {
                setNodeProperty(context, locationResourceTemplate, nodeTemplate, matchingConfiguration);
            }
        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
            throw new ConstraintTechnicalException("Dispatching constraint violation.", e);
        }
    }

    private void setNodeProperty(FlowExecutionContext context, LocationResourceTemplate locationResourceTemplate, NodeTemplate nodeTemplate,
            DeploymentMatchingConfiguration matchingConfiguration) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        PropertyDefinition propertyDefinition = ToscaContext.getOrFail(NodeType.class, locationResourceTemplate.getTemplate().getType()).getProperties()
                .get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException("No property of name <" + propertyName + "> can be found on the node template <" + nodeTemplateId + "> of type <"
                    + locationResourceTemplate.getTemplate().getType() + ">");
        }

        AbstractPropertyValue locationResourcePropertyValue = locationResourceTemplate.getTemplate().getProperties().get(propertyName);
        ensureNotSet(locationResourcePropertyValue, "by the admin in the Location Resource Template", propertyName, propertyValue);
        ensureNotSet(nodeTemplate.getProperties().get(propertyName), "in the portable topology", propertyName, propertyValue);

        // Update the configuration
        NodePropsOverride nodePropsOverride = getNodePropsOverride(matchingConfiguration);

        // Perform the update of the property
        if (propertyValue == null) {
            nodePropsOverride.getProperties().remove(propertyName);
        } else {
            ConstraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);
            nodePropsOverride.getProperties().put(propertyName, PropertyService.asPropertyValue(propertyValue));
        }

        context.saveConfiguration(matchingConfiguration);
    }

    private void setNodeCapabilityProperty(FlowExecutionContext context, LocationResourceTemplate locationResourceTemplate, NodeTemplate nodeTemplate,
            String capabilityName, DeploymentMatchingConfiguration matchingConfiguration)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        Capability locationResourceCapability = locationResourceTemplate.getTemplate().getCapabilities().get(capabilityName);
        if (locationResourceCapability == null) {
            throw new NotFoundException("The capability <" + capabilityName + "> cannot be found on node template <" + nodeTemplateId + "> of type <"
                    + locationResourceTemplate.getTemplate().getType() + ">");
        }
        PropertyDefinition propertyDefinition = ToscaContext.getOrFail(CapabilityType.class, locationResourceCapability.getType()).getProperties()
                .get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException("No property with name <" + propertyName + "> can be found on capability <" + capabilityName + "> of type <"
                    + locationResourceCapability.getType() + ">");
        }

        AbstractPropertyValue locationResourcePropertyValue = locationResourceTemplate.getTemplate().getCapabilities().get(capabilityName).getProperties()
                .get(propertyName);
        ensureNotSet(locationResourcePropertyValue, "by the admin in the Location Resource Template", propertyName, propertyValue);
        AbstractPropertyValue originalNodePropertyValue = safe(nodeTemplate.getCapabilities().get(capabilityName).getProperties()).get(propertyName);
        ensureNotSet(originalNodePropertyValue, "in the portable topology", propertyName, propertyValue);

        // Update the configuration
        NodePropsOverride nodePropsOverride = getNodePropsOverride(matchingConfiguration);
        if (propertyValue == null && nodePropsOverride.getCapabilities().get(capabilityName) != null) {
            nodePropsOverride.getCapabilities().get(capabilityName).getProperties().remove(propertyName);
        } else {
            // Set check constraints
            ConstraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);

            if (nodePropsOverride.getCapabilities().get(capabilityName) == null) {
                nodePropsOverride.getCapabilities().put(capabilityName, new NodeCapabilitiesPropsOverride());
            }
            nodePropsOverride.getCapabilities().get(capabilityName).getProperties().put(propertyName, PropertyService.asPropertyValue(propertyValue));
        }

        context.saveConfiguration(matchingConfiguration);
    }

    private NodePropsOverride getNodePropsOverride(DeploymentMatchingConfiguration matchingConfiguration) {
        NodePropsOverride nodePropsOverride = matchingConfiguration.getMatchedNodesConfiguration().get(nodeTemplateId);
        if (nodePropsOverride == null) {
            nodePropsOverride = new NodePropsOverride();
            matchingConfiguration.getMatchedNodesConfiguration().put(nodeTemplateId, nodePropsOverride);
        }
        return nodePropsOverride;
    }

    /**
     * Check that the property is not already defined in a source
     *
     * @param sourcePropertyValue null or an already defined Property Value.
     * @param messageSource The named source to add in the exception message in case of failure.
     */
    @SneakyThrows(IntrospectionException.class) // This cannot be thrown on getConstraintInformation from an equals constraint.
    private void ensureNotSet(AbstractPropertyValue sourcePropertyValue, String messageSource, String propertyName, Object propertyValue)
            throws ConstraintViolationException {
        if (sourcePropertyValue != null) {
            EqualConstraint constraint = new EqualConstraint();
            if (sourcePropertyValue instanceof ScalarPropertyValue) {
                constraint.setEqual(((ScalarPropertyValue) sourcePropertyValue).getValue());
            }
            ConstraintUtil.ConstraintInformation information = ConstraintUtil.getConstraintInformation(constraint);
            // If admin has defined a value users should not be able to override it.
            throw new ConstraintViolationException("Overriding value specified " + messageSource + " is not authorized.", null, information);
        }
    }
}
