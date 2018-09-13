package org.alien4cloud.tosca.utils;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.ComplexPropertyValue;
import org.alien4cloud.tosca.model.definitions.ConcatPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.ListPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.AbstractInstantiableTemplate;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.Requirement;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;

import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to perform function evaluation on a topology template.
 */
@Slf4j
public class FunctionEvaluator {

    /**
     * Try to resolve the actual property value
     *
     * @param evaluatorContext The evaluation context, Topology and Inputs.
     * @param template The template that owns the given properties (node template properties or relationship properties) or the Capability/Requirement that ows
     *            the properties (node template).
     * @param properties The properties of the entity for which to resolve a specific property.
     * @param evaluatedProperty The property to resolve (should be one of the properties element but this won't be checked here).
     * @return The evaluated value for the property.
     */
    public static AbstractPropertyValue tryResolveValue(FunctionEvaluatorContext evaluatorContext, AbstractInstantiableTemplate template,
            Map<String, AbstractPropertyValue> properties, AbstractPropertyValue evaluatedProperty) {
        if (evaluatedProperty == null) {
            // There is nothing to be evaluated.
            return null;
        }
        if (evaluatedProperty instanceof PropertyValue) {
            // This is a value already so just return it.
            return evaluatedProperty;
        }
        if (containGetSecretFunction(evaluatedProperty)) {
            return evaluatedProperty;
        }
        if (evaluatedProperty instanceof FunctionPropertyValue) {
            FunctionPropertyValue evaluatedFunction = (FunctionPropertyValue) evaluatedProperty;
            switch (evaluatedFunction.getFunction()) {
            case ToscaFunctionConstants.GET_INPUT:
                String inputName = evaluatedFunction.getParameters().get(0);
                return evaluatorContext.getInputs().get(inputName);
            case ToscaFunctionConstants.GET_PROPERTY:
                // Perform the get property evaluation.
                return getProperty(evaluatorContext, template, properties, evaluatedFunction);
            default:
                throw new IllegalArgumentException("GET_ATTRIBUTE or GET_OPERATION_OUTPUT cannot be defined on a property.");
            }
        }
        if (evaluatedProperty instanceof ConcatPropertyValue) {
            // Perform the concat evaluation
            return concat(evaluatorContext, template, properties, (ConcatPropertyValue) evaluatedProperty);
        }
        throw new IllegalArgumentException("AbstractPropertyValue must be one of null, a secret, PropertyValue, FunctionPropertyValue or ConcatPropertyValue");
    }

    /**
     * Process a get_property function against the topology.
     * 
     * @param evaluatorContext The evaluation context, Topology and Inputs.
     * @param template The template that owns the given properties (node template properties or relationship properties) or the Capability/Requirement that ows
     *            the properties (node template).
     * @param properties The properties of the entity for which to resolve a specific property.
     * @param function The function (must be a get property).
     * @return The abstract property value associated with the get_property function.
     */
    private static AbstractPropertyValue getProperty(FunctionEvaluatorContext evaluatorContext, AbstractInstantiableTemplate template,
            Map<String, AbstractPropertyValue> properties, FunctionPropertyValue function) {
        switch (function.getTemplateName()) {
        case ToscaFunctionConstants.SELF:
            if (properties != null) {
                AbstractPropertyValue propertyValue = getFromPath(evaluatorContext, template, properties, function.getElementNameToFetch());
                if (propertyValue != null) {
                    return tryResolveValue(evaluatorContext, template, properties, propertyValue);
                }
            }
            return doGetProperty(evaluatorContext, template, function);
        case ToscaFunctionConstants.HOST:
            if (template instanceof NodeTemplate) {
                return doGetProperty(evaluatorContext,
                        TopologyNavigationUtil.getImmediateHostTemplate(evaluatorContext.getTopology(), (NodeTemplate) template), function);
            } else {
                throw new IllegalArgumentException("HOST keyname cannot be used if not in a node template context (or capability/requirement).");
            }
            // TODO implement get_property from SOURCE.
            // case ToscaFunctionConstants.SOURCE:
            // return doGetProperty(topology, TopologyNavigationUtil.getImmediateHostTemplate(topology, functionTemplate), null, function);
        case ToscaFunctionConstants.TARGET:
            Set<NodeTemplate> targetNodes = TopologyNavigationUtil.getTargetNodes(evaluatorContext.getTopology(), (NodeTemplate) template, function.getCapabilityOrRequirementName());
            if (targetNodes != null && targetNodes.size() == 1) {
                NodeTemplate firstNode = targetNodes.iterator().next();
                List<String> params = new ArrayList<>();
                params.add(ToscaFunctionConstants.SELF);
                params.addAll(function.getParameters().subList(2, function.getParameters().size()));
                FunctionPropertyValue newFunc = new FunctionPropertyValue(function.getFunction(), params);
                return tryResolveValue(evaluatorContext, firstNode, firstNode.getProperties(), newFunc);
            }
        default:
            return doGetProperty(evaluatorContext, evaluatorContext.getTopology().getNodeTemplates().get(function.getTemplateName()), function);
        }
    }

    private static AbstractPropertyValue doGetProperty(FunctionEvaluatorContext evaluatorContext, AbstractInstantiableTemplate targetTemplate,
            FunctionPropertyValue function) {
        if (targetTemplate == null) {
            return null;
        }
        // If a requirement or capability name is defined then it is applied to the node template.
        if (function.getCapabilityOrRequirementName() != null) {
            if (targetTemplate instanceof RelationshipTemplate) {
                throw new IllegalArgumentException(
                        "Get property that specifies a capability or relationship target must be placed on a node template and not a relationship template.");
            }

            AbstractPropertyValue propertyValue = null;
            Capability targetCapability = safe(((NodeTemplate) targetTemplate).getCapabilities()).get(function.getCapabilityOrRequirementName());
            if (targetCapability != null) {
                propertyValue = getFromPath(evaluatorContext, targetTemplate, targetCapability.getProperties(), function.getElementNameToFetch());
            }

            if (propertyValue == null) {
                Requirement requirement = safe(((NodeTemplate) targetTemplate).getRequirements()).get(function.getCapabilityOrRequirementName());
                if (requirement != null) {
                    propertyValue = getFromPath(evaluatorContext, targetTemplate, requirement.getProperties(), function.getElementNameToFetch());
                }
            }

            if (propertyValue == null) {
                // try to find the value from the host node.
                propertyValue = doGetProperty(evaluatorContext,
                        TopologyNavigationUtil.getImmediateHostTemplate(evaluatorContext.getTopology(), (NodeTemplate) targetTemplate), function);
            }

            return tryResolveValue(evaluatorContext, targetTemplate, targetTemplate.getProperties(), propertyValue);
        }
        // Try to fetch from the node.
        AbstractPropertyValue propertyValue = getFromPath(evaluatorContext, targetTemplate, targetTemplate.getProperties(), function.getElementNameToFetch());
        if (propertyValue == null && targetTemplate instanceof NodeTemplate) {
            propertyValue = doGetProperty(evaluatorContext,
                    TopologyNavigationUtil.getImmediateHostTemplate(evaluatorContext.getTopology(), (NodeTemplate) targetTemplate), function);
        }

        // if the property refers to a function (get_input/get_property then try to resolve it).
        return tryResolveValue(evaluatorContext, targetTemplate, targetTemplate.getProperties(), propertyValue);
    }

    private static AbstractPropertyValue getFromPath(FunctionEvaluatorContext evaluatorContext, AbstractInstantiableTemplate targetTemplate,
            Map<String, AbstractPropertyValue> properties, String propertyPath) {
        if (propertyPath.contains(".")) {
            String propertyName = propertyPath.split("\\.")[0];
            AbstractPropertyValue propertyValue = properties.get(propertyName);
            if (!(propertyValue instanceof PropertyValue)) {
                // if the value is not a property value resolve it first
                propertyValue = tryResolveValue(evaluatorContext, targetTemplate, properties, propertyValue);
                if (propertyValue == null) {
                    return null;
                }
            }
            // now it is a property value
            Object value = MapUtil.get(((PropertyValue) propertyValue).getValue(), propertyPath.substring(propertyName.length() + 1));
            if (value == null) {
                return null;
            } else if (value instanceof String) {
                return new ScalarPropertyValue((String) value);
            } else if (value instanceof List) {
                return new ListPropertyValue((List<Object>) value);
            } else if (value instanceof Map) {
                return new ComplexPropertyValue((Map<String, Object>) value);
            }
            throw new IllegalArgumentException("The value of a property must be a scalar, a list or a map.");
        }

        return safe(properties).get(propertyPath);
    }

    private static AbstractPropertyValue concat(FunctionEvaluatorContext evaluatorContext, AbstractInstantiableTemplate template,
            Map<String, AbstractPropertyValue> properties, ConcatPropertyValue concatPropertyValue) {
        StringBuilder sb = new StringBuilder();

        for (AbstractPropertyValue abstractPropertyValue : concatPropertyValue.getParameters()) {
            AbstractPropertyValue propertyValue = tryResolveValue(evaluatorContext, template, properties, abstractPropertyValue);
            if (propertyValue == null) {
                // Ignore this as it may be a null default value
            } else if (propertyValue instanceof ScalarPropertyValue) {
                sb.append(((ScalarPropertyValue) propertyValue).getValue());
            } else if (propertyValue instanceof ListPropertyValue) {
                for (Object listValue : ((ListPropertyValue) propertyValue).getValue()) {
                    if (listValue instanceof String) {
                        sb.append(listValue);
                    } else {
                        throw new IllegalArgumentException("Concat can only be resolved for strings or list of strings.");
                    }
                }
            } else if (propertyValue instanceof FunctionPropertyValue
                    && ToscaFunctionConstants.GET_INPUT.equals(((FunctionPropertyValue) propertyValue).getFunction())) {
                // just ignore that as unprocessed inputs are from voluntary callback in the evaluator context for later processing.
                log.debug("Ignoring contact of an unresolved get_input function.");
            } else {
                throw new IllegalArgumentException("Concat can only be resolved for strings or list of strings.");
            }
        }

        return new ScalarPropertyValue(sb.toString());
    }

    /**
     * Check whether the value contains a get_secret
     *
     * @param propertyValue the value of the property
     * @return true if the property's value is a function and contains a get_secret
     */
    public static boolean containGetSecretFunction(AbstractPropertyValue propertyValue) {
        if (propertyValue instanceof FunctionPropertyValue) {
            if (ToscaFunctionConstants.GET_SECRET.equals(((FunctionPropertyValue) propertyValue).getFunction())) {
                return true;
            }
        } else if (propertyValue instanceof ConcatPropertyValue) {
            return ((ConcatPropertyValue) propertyValue).getParameters().stream().anyMatch(FunctionEvaluator::containGetSecretFunction);
        }
        return false;
    }
}