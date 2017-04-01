package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;

/**
 * Utility class to perform function evaluation on a topology.
 */
public class FunctionEvaluator {
    /**
     * Process a get_property function against the topology.
     * 
     * @param topology The topology on which to process the function.
     * @param functionTemplate The template on which the function is defined.
     * @param functionCapability The (optional) capability on which the function is defined.
     * @param function The function (must be a get property).
     * @return The abstract property value associated with the get_property function.
     */
    public static AbstractPropertyValue getProperty(Topology topology, NodeTemplate functionTemplate, Capability functionCapability,
            FunctionPropertyValue function) {
        if (ToscaFunctionConstants.GET_PROPERTY.equals(function.getFunction())) {
            switch (function.getTemplateName()) {
            case ToscaFunctionConstants.SELF:
                if (functionCapability != null) {
                    AbstractPropertyValue propertyValue = safe(functionCapability.getProperties()).get(function.getElementNameToFetch());
                    if (propertyValue != null) {
                        return propertyValue;
                    }
                }
                return doGetProperty(topology, functionTemplate, function);
            case ToscaFunctionConstants.HOST:
                return doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, functionTemplate), function);
            // TODO handle relationship property management here.
            // case ToscaFunctionConstants.SOURCE:
            // return doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, functionTemplate), null, function);
            // case ToscaFunctionConstants.TARGET:
            // return doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, functionTemplate), null, function);
            default:
                doGetProperty(topology, functionTemplate, function);
            }
        }
        return null;
    }

    private static AbstractPropertyValue doGetProperty(Topology topology, NodeTemplate targetTemplate, FunctionPropertyValue function) {
        if (targetTemplate == null) {
            return null;
        }
        // If a requirement or capability name is defined then it is applied to the node template.
        if (function.getCapabilityOrRequirementName() != null) {
            AbstractPropertyValue propertyValue = null;
            Capability targetCapability = safe(targetTemplate.getCapabilities()).get(function.getCapabilityOrRequirementName());
            if (targetCapability != null) {
                propertyValue = safe(targetCapability.getProperties()).get(function.getElementNameToFetch());
            }

            if (propertyValue == null) {
                Requirement requirement = safe(targetTemplate.getRequirements()).get(function.getCapabilityOrRequirementName());
                if (requirement != null) {
                    propertyValue = safe(requirement.getProperties()).get(function.getElementNameToFetch());
                }
            }

            if (propertyValue == null) {
                // try to find the value from the host node.
                propertyValue = doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, targetTemplate), function);
            }

            return propertyValue;
        }
        // Try to fetch from the node.
        AbstractPropertyValue propertyValue = safe(targetTemplate.getProperties()).get(function.getElementNameToFetch());
        if (propertyValue == null) {
            propertyValue = doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, targetTemplate), function);
        }
        return propertyValue;
    }
}