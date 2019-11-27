package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import java.beans.IntrospectionException;
import java.util.Map;
import java.util.Optional;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.ResourceMatching;
import org.alien4cloud.tosca.exceptions.ConstraintTechnicalException;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.orchestrators.locations.AbstractLocationResourceTemplate;
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
public abstract class AbstractSetMatchedPropertyModifier<T extends AbstractInheritableToscaType, U extends AbstractTemplate, V extends AbstractLocationResourceTemplate<U>>
        implements ITopologyModifier {
    protected PropertyService propertyService;
    protected String templateId;
    protected String propertyName;
    protected Object propertyValue;

    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingConfigAutoSelectModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        Map<String, ResourceMatching> lastUserSubstitutions = getUserMatches(matchingConfiguration);
        U template = getTemplates(topology).get(templateId);

        if (template == null) {
            throw new NotFoundException("Topology [" + topology.getId() + "] does not contains any " + getSubject() + " with id [" + templateId + "]");
        }
        String substitutionId = lastUserSubstitutions.get(templateId).getResourceId();
        if (substitutionId == null) {
            throw new NotFoundException("The " + getSubject() + " [" + templateId + "] from topology [" + topology.getId() + "] is not matched.");
        }

        Map<String, V> allAvailableResourceTemplates = getAvailableResourceTemplates(context);
        V resourceTemplate = allAvailableResourceTemplates.get(substitutionId);

        try {
            setProperty(context, resourceTemplate, template, matchingConfiguration);
        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
            throw new ConstraintTechnicalException("Dispatching constraint violation.", e);
        }
    }

    protected void setProperty(FlowExecutionContext context, V resourceTemplate, U template, DeploymentMatchingConfiguration matchingConfiguration)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        PropertyDefinition propertyDefinition = ((T) ToscaContext.getOrFail(AbstractToscaType.class, resourceTemplate.getTemplate().getType())).getProperties()
                .get(propertyName);
        if (propertyDefinition == null) {
            throw new NotFoundException("No property of name [" + propertyName + "] can be found on the " + getSubject() + " template [" + templateId
                    + "] of type [" + resourceTemplate.getTemplate().getType() + "]");
        }

        AbstractPropertyValue locationResourcePropertyValue = resourceTemplate.getTemplate().getProperties().get(propertyName);
        ensureNotSet(locationResourcePropertyValue, "by the admin in the Location Resource Template", propertyName, propertyValue);
        ensureNotSet(template.getProperties().get(propertyName), "in the portable topology", propertyName, propertyValue);

        // Update the configuration
        NodePropsOverride nodePropsOverride = getTemplatePropsOverride(matchingConfiguration);

        // Perform the update of the property
        if (propertyValue == null) {
            nodePropsOverride.getProperties().remove(propertyName);
        } else {
            AbstractPropertyValue abstractPropertyValue = PropertyService.asPropertyValue(propertyValue);
            if (! (abstractPropertyValue instanceof FunctionPropertyValue)) {
                ConstraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);
            }
            nodePropsOverride.getProperties().put(propertyName, abstractPropertyValue);
        }

        context.saveConfiguration(matchingConfiguration);
    }

    protected NodePropsOverride getTemplatePropsOverride(DeploymentMatchingConfiguration matchingConfiguration) {
        return getPropertiesOverrides(matchingConfiguration).computeIfAbsent(templateId, k -> new NodePropsOverride());
    }

    /**
     * Check that the property is not already defined in a source
     *
     * @param sourcePropertyValue null or an already defined Property Value.
     * @param messageSource The named source to add in the exception message in case of failure.
     */
    @SneakyThrows(IntrospectionException.class) // This cannot be thrown on getConstraintInformation from an equals constraint.
    protected void ensureNotSet(AbstractPropertyValue sourcePropertyValue, String messageSource, String propertyName, Object propertyValue)
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

    abstract Map<String, ResourceMatching> getUserMatches(DeploymentMatchingConfiguration matchingConfiguration);

    abstract Map<String, V> getAvailableResourceTemplates(FlowExecutionContext context);

    abstract Map<String, NodePropsOverride> getPropertiesOverrides(DeploymentMatchingConfiguration matchingConfiguration);

    abstract String getSubject();

    abstract Map<String, U> getTemplates(Topology topology);

}
