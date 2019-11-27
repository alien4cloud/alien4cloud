package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodeCapabilitiesPropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.ResourceMatching;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.utils.services.ConstraintPropertyService;
import alien4cloud.utils.services.PropertyService;

/**
 * This modifier is injected when the deployment cycle is run in the context of a deployment user update to the properties of a matched node.
 *
 * It injects the custom user defined property in the node if valid.
 */
public class SetMatchedNodePropertyModifier extends AbstractSetMatchedPropertyModifier<NodeType, NodeTemplate, LocationResourceTemplate> {
    private Optional<String> optionalCapabilityName;

    public SetMatchedNodePropertyModifier(PropertyService propertyService, String templateId, String propertyName, Object propertyValue,
            Optional<String> optionalCapabilityName) {
        super(propertyService, templateId, propertyName, propertyValue);
        this.optionalCapabilityName = optionalCapabilityName;
    }

    private void setNodeCapabilityProperty(FlowExecutionContext context, LocationResourceTemplate locationResourceTemplate, NodeTemplate nodeTemplate,
            String capabilityName, DeploymentMatchingConfiguration matchingConfiguration)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        Capability locationResourceCapability = locationResourceTemplate.getTemplate().getCapabilities().get(capabilityName);
        if (locationResourceCapability == null) {
            throw new NotFoundException("The capability <" + capabilityName + "> cannot be found on node template <" + templateId + "> of type <"
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
        NodePropsOverride nodePropsOverride = getTemplatePropsOverride(matchingConfiguration);
        if (propertyValue == null && nodePropsOverride.getCapabilities().get(capabilityName) != null) {
            nodePropsOverride.getCapabilities().get(capabilityName).getProperties().remove(propertyName);
        } else {
            // Set check constraints
            ConstraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);

            NodeCapabilitiesPropsOverride nodeCapabilitiesPropsOverride = nodePropsOverride.getCapabilities().computeIfAbsent(capabilityName,
                    k -> new NodeCapabilitiesPropsOverride());
            nodeCapabilitiesPropsOverride.getProperties().put(propertyName, PropertyService.asPropertyValue(propertyValue));
        }

        context.saveConfiguration(matchingConfiguration);
    }

    @Override
    protected void setProperty(FlowExecutionContext context, LocationResourceTemplate resourceTemplate, NodeTemplate template,
            DeploymentMatchingConfiguration matchingConfiguration) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {

        if (optionalCapabilityName.isPresent()) {
            setNodeCapabilityProperty(context, resourceTemplate, template, optionalCapabilityName.get(), matchingConfiguration);
        } else {
            super.setProperty(context, resourceTemplate, template, matchingConfiguration);
        }
    }

    @Override
    Map<String, ResourceMatching> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedLocationResources();
    }

    @Override
    Map<String, LocationResourceTemplate> getAvailableResourceTemplates(FlowExecutionContext context) {
        return (Map<String, LocationResourceTemplate>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP);
    }

    @Override
    Map<String, NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedNodesConfiguration();
    }

    @Override
    String getSubject() {
        return "node";
    }

    @Override
    Map<String, NodeTemplate> getTemplates(Topology topology) {
        return topology.getNodeTemplates();
    }
}
