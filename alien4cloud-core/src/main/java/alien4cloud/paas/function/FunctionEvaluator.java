package alien4cloud.paas.function;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.AttributeDefinition;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IAttributeValue;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.paas.exception.PaaSTechnicalException;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.ToscaFunctionConstants;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Utility class to process functions defined in attributes level:
 * ex:
 * attributes:
 * url: "http://get_property: [the_node_tempalte_1, the_property_name_1]:get_property: [the_node_tempalte_2, the_property_name_2 ]/super"
 */
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class FunctionEvaluator {

    /**
     * Parse an attribute value that can be : {@link ConcatPropertyValue} / {@link AttributeDefinition}
     *
     * @param attributeId
     * @param attributeValue
     * @param topology
     * @param runtimeInformations
     * @param currentInstance
     * @param basePaaSTemplate
     * @param builtPaaSTemplates
     * @return
     */
    public static String parseAttribute(String attributeId, IAttributeValue attributeValue, Topology topology,
            Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance,
            IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate, Map<String, PaaSNodeTemplate> builtPaaSTemplates) {

        if (attributeValue == null) {
            return null;
        }

        // handle AttributeDefinition type
        if (attributeValue instanceof AttributeDefinition) {
            String runtimeAttributeValue = extractRuntimeInformationAttribute(runtimeInformations, currentInstance, Lists.newArrayList(basePaaSTemplate),
                    attributeId);
            if (runtimeAttributeValue != null) {
                if (!runtimeAttributeValue.contains("=Error!]") && !runtimeAttributeValue.equals("") && !runtimeAttributeValue.equals(null)) {
                    return runtimeAttributeValue;
                }
            }

            return ((AttributeDefinition) attributeValue).getDefault();
        }

        // handle concat function
        StringBuilder evaluatedAttribute = new StringBuilder();
        ConcatPropertyValue concatPropertyValue = (ConcatPropertyValue) attributeValue;
        for (IOperationParameter concatParam : concatPropertyValue.getParameters()) {
            // scalar type
            if (concatParam instanceof ScalarPropertyValue) {
                // scalar case
                evaluatedAttribute.append(((ScalarPropertyValue) concatParam).getValue());
            } else if (concatParam instanceof PropertyDefinition) {
                // Definition case
                // TODO : ?? what should i do here ?? currently returns default value in the definition
                evaluatedAttribute.append(((PropertyDefinition) concatParam).getDefault());
            } else if (concatParam instanceof FunctionPropertyValue) {
                // Function case
                List<? extends IPaaSTemplate> paasTemplates = null;
                String propertyOrAttributeName = null;
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) concatParam;
                paasTemplates = getPaaSTemplatesFromKeyword(basePaaSTemplate, functionPropertyValue.getTemplateName(), builtPaaSTemplates);
                switch (functionPropertyValue.getFunction()) {
                case ToscaFunctionConstants.GET_ATTRIBUTE:
                    evaluatedAttribute.append(extractRuntimeInformationAttribute(runtimeInformations, currentInstance, paasTemplates, propertyOrAttributeName));
                    break;
                case ToscaFunctionConstants.GET_PROPERTY:
                    evaluatedAttribute.append(extractRuntimeInformationProperty(topology, functionPropertyValue.getPropertyOrAttributeName(), paasTemplates));
                    break;
                default:
                    log.warn("Function [{}] is not yet handled in concat operation.", functionPropertyValue.getFunction());
                }

            }
        }
        return evaluatedAttribute.toString();
    }

    /**
     * Extract property value from runtime informations
     *
     * @param topology
     * @param propertyOrAttributeName
     * @param nodes
     * @return
     */
    private static String extractRuntimeInformationProperty(Topology topology, String propertyOrAttributeName, List<? extends IPaaSTemplate> nodes) {
        AbstractPropertyValue propertyOrAttributeValue;
        NodeTemplate template = null;
        for (IPaaSTemplate node : nodes) {
            String nodeName = node.getId();
            template = topology.getNodeTemplates().get(nodeName);
            if (template != null && template.getProperties() != null) {
                propertyOrAttributeValue = template.getProperties().get(propertyOrAttributeName);
                if (propertyOrAttributeValue != null) {
                    return getScalarValue(propertyOrAttributeValue);
                }
            }
        }
        log.warn("Couldn't find property <{}> of node <{}>", propertyOrAttributeName, nodes);
        return "[" + nodes + "." + propertyOrAttributeName + "=Error!]";
    }

    /**
     * Return the first matching value in parent nodes hierarchy
     *
     * @param runtimeInformations
     * @param currentInstance
     * @param nodes
     * @param propertyOrAttributeName
     * @return runtime value
     */
    private static String extractRuntimeInformationAttribute(Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance,
            List<? extends IPaaSTemplate> nodes, String propertyOrAttributeName) {
        Map<String, String> attributes = null;
        // return the first found
        for (IPaaSTemplate node : nodes) {
            String nodeName = node.getId();
            // get the current attribute value
            if (runtimeInformations.get(nodeName) != null) {
                // get value for an instance if instance number found
                if (runtimeInformations.get(nodeName).containsKey(currentInstance)) {
                    attributes = runtimeInformations.get(nodeName).get(currentInstance).getAttributes();
                } else {
                    attributes = runtimeInformations.get(nodeName).entrySet().iterator().next().getValue().getAttributes();
                }
                if (attributes.containsKey(propertyOrAttributeName)) {
                    return attributes.get(propertyOrAttributeName);
                }
            }
        }
        log.warn("Couldn't find attribute <{}> in nodes <{}>", propertyOrAttributeName, nodes.toString());
        return "<" + propertyOrAttributeName + ">"; // value not yet computed (or won't be computes)
    }

    /**
     * Return the paaS entities based on a keyword. This latest can be a special keyword (SELF, SOURCE, TARGET, HOST), or a node template name
     *
     * @param basePaaSTemplate The base PaaSTemplate for which to get the entity name
     * @param keyword The
     * @param builtPaaSTemplates
     * @return a list of PaaSTemplate(relationship or node) resulting from the evaluation
     */
    public static List<? extends IPaaSTemplate> getPaaSTemplatesFromKeyword(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate, String keyword,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        switch (keyword) {
        case ToscaFunctionConstants.HOST:
            return getParentsNodes(basePaaSTemplate);
        case ToscaFunctionConstants.SELF:
            return Lists.<IPaaSTemplate> newArrayList(basePaaSTemplate);
        case ToscaFunctionConstants.SOURCE:
            return Lists.<IPaaSTemplate> newArrayList(getSourceNode(basePaaSTemplate, builtPaaSTemplates));
        case ToscaFunctionConstants.TARGET:
            return Lists.<IPaaSTemplate> newArrayList(getTargetNode(basePaaSTemplate, builtPaaSTemplates));
        default:
            return Lists.<IPaaSTemplate> newArrayList(getPaaSNodeOrFail(keyword, builtPaaSTemplates));
        }
    }

    /**
     * Evaluate a get_property function type
     *
     * @param functionParam The property function of type {@link FunctionPropertyValue} to evaluate
     * @param basePaaSTemplate The base PaaSTemplate in which the parameter is defined. Can be a {@link PaaSRelationshipTemplate} or a {@link PaaSNodeTemplate}.
     * @param builtPaaSTemplates A map < {@link String}, {@link PaaSNodeTemplate}> of built nodetemplates of the processed topology. Note that these
     *            {@link PaaSNodeTemplate}s should have been built, thus referencing their related parents and relationships.
     * @return the String result of the function evalutation
     */
    public static String evaluateGetPropertyFuntion(FunctionPropertyValue functionParam, IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        List<? extends IPaaSTemplate> paaSTemplates = getPaaSTemplatesFromKeyword(basePaaSTemplate, functionParam.getTemplateName(), builtPaaSTemplates);
        String propertyId = functionParam.getPropertyOrAttributeName();
        for (IPaaSTemplate paaSTemplate : paaSTemplates) {
            AbstractPropertyValue propertyValue = getPropertyFromTemplateOrCapability(paaSTemplate, functionParam.getCapabilityOrRequirementName(),
                    functionParam.getPropertyOrAttributeName());
            // return the first value found
            if (propertyValue != null) {
                if (propertyValue instanceof ScalarPropertyValue) {
                    return ((ScalarPropertyValue) propertyValue).getValue();
                } else {
                    throw new FunctionEvaluationException("Failed to evaluate the property <" + propertyId + "> of node <" + basePaaSTemplate.getId()
                            + ">. 'get_property' / 'get_attribute' functions are not supported on node's entities' properties definition.");
                }
            }
        }
        return null;
    }

    /**
     * Find a property from a template or capability / requirement if a name is provided
     * first find in capability, and then in requirement if no found.
     *
     * @param paaSTemplate
     * @param capabilityOrRequirementName
     * @param elementName
     * @return
     */
    private static AbstractPropertyValue getPropertyFromTemplateOrCapability(IPaaSTemplate paaSTemplate, String capabilityOrRequirementName, String elementName) {

        // if no capability or requirement provided, return the value from the template property
        if (StringUtils.isBlank(capabilityOrRequirementName)) {
            return paaSTemplate.getTemplate().getProperties().get(elementName);
        } else if (paaSTemplate instanceof PaaSNodeTemplate) {
            // if capability or requirement name provided:
            // FIXME how should I know that the provided name is capability or a requirement name?
            NodeTemplate nodeTemplate = (NodeTemplate) paaSTemplate.getTemplate();
            AbstractPropertyValue propertyValue = null;

            Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
            Map<String, Requirement> requirements = nodeTemplate.getRequirements();

            // Find in capability first
            if (capabilities != null && capabilities.get(capabilityOrRequirementName) != null
                    && capabilities.get(capabilityOrRequirementName).getProperties() != null) {
                propertyValue = capabilities.get(capabilityOrRequirementName).getProperties().get(elementName);
            }

            // if not found in capability, find in requirement
            if (propertyValue == null) {
                if (requirements != null && requirements.containsKey(capabilityOrRequirementName)
                        && requirements.get(capabilityOrRequirementName).getProperties() != null) {
                    propertyValue = requirements.get(capabilityOrRequirementName).getProperties().get(elementName);
                }
            }

            return propertyValue;
        }

        log.warn("The keyword <" + ToscaFunctionConstants.SELF
                + "> can not be used on a Relationship Template level's parameter when trying to retrieve capability / requiement properties. Node<"
                + paaSTemplate.getId() + ">");
        return null;
    }

    private static PaaSNodeTemplate getPaaSNodeOrFail(String nodeId, Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        PaaSNodeTemplate toReturn = builtPaaSTemplates.get(nodeId);
        if (toReturn == null) {
            throw new FunctionEvaluationException(" Failled to retrieve the nodeTemplate with name <" + nodeId + ">");
        }
        return toReturn;
    }

    private static List<? extends IPaaSTemplate> getParentsNodes(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (!(basePaaSTemplate instanceof PaaSNodeTemplate)) {
            throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.HOST + "> can only be used on a NodeTemplate level's parameter. Node<"
                    + basePaaSTemplate.getId() + ">");
        }
        // TODO Must review this management of host
        PaaSNodeTemplate template = (PaaSNodeTemplate) basePaaSTemplate;
        if (template.getParent() == null) {
            return Lists.<IPaaSTemplate> newArrayList(template);
        }
        try {
            List<? extends IPaaSTemplate> parentList = ToscaUtils.getParents(template);
            return parentList;
        } catch (PaaSTechnicalException e) {
            throw new FunctionEvaluationException("Failed to retrieve the root node of <" + basePaaSTemplate.getId() + ">.", e);
        }
    }

    private static IPaaSTemplate<? extends IndexedToscaElement> getSourceNode(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return getPaaSNodeOrFail(((PaaSRelationshipTemplate) basePaaSTemplate).getSource(), builtPaaSTemplates);
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.SOURCE + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static IPaaSTemplate<? extends IndexedToscaElement> getTargetNode(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return getPaaSNodeOrFail(((PaaSRelationshipTemplate) basePaaSTemplate).getRelationshipTemplate().getTarget(), builtPaaSTemplates);
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.TARGET + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">.");
    }

    /**
     * Get the scalar value
     *
     * @param propertyValue the property value
     * @throws alien4cloud.paas.exception.NotSupportedException if called on a non ScalarPropertyValue
     * @return the value or null if the propertyValue is null
     */
    public static String getScalarValue(AbstractPropertyValue propertyValue) {
        if (propertyValue == null) {
            return null;
        } else if (propertyValue instanceof ScalarPropertyValue) {
            return ((ScalarPropertyValue) propertyValue).getValue();
        } else {
            throw new NotSupportedException("Property value is not of type scalar");
        }
    }

    public static Map<String, String> getScalarValues(Map<String, AbstractPropertyValue> propertyValues) {
        if (propertyValues == null) {
            return null;
        }
        Map<String, String> properties = Maps.newHashMap();
        for (Map.Entry<String, AbstractPropertyValue> propertyValueEntry : propertyValues.entrySet()) {
            properties.put(propertyValueEntry.getKey(), getScalarValue(propertyValueEntry.getValue()));
        }
        return properties;
    }

    public static boolean isGetAttribute(FunctionPropertyValue function) {
        return ToscaFunctionConstants.GET_ATTRIBUTE.equals(function.getFunction());
    }
}