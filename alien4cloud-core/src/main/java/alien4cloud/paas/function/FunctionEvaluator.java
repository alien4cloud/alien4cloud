package alien4cloud.paas.function;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.common.AlienConstants;
import alien4cloud.model.components.*;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Requirement;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSTemplate;
import alien4cloud.paas.exception.NotSupportedException;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.ToscaUtils;
import alien4cloud.tosca.normative.ToscaFunctionConstants;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to process functions defined in attributes or operations input level:
 * ex:<br>
 *
 * <pre>
 * attributes:
 *   url: "http://get_property: [the_node_tempalte_1, the_property_name_1]:get_property: [the_node_tempalte_2, the_property_name_2 ]/super"
 * </pre>
 */
@Slf4j
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class FunctionEvaluator {

    /**
     * Post process / enrich instance information by parsing all function in attributes and replacing them with real values
     *
     * @param instanceInformations the instance information to post process
     * @param topology the topology
     * @param paaSTopology the pass topology
     */
    public static void postProcessInstanceInformation(Map<String, Map<String, InstanceInformation>> instanceInformations, Topology topology,
            PaaSTopology paaSTopology) {
        // parse attributes
        for (Map.Entry<String, Map<String, InstanceInformation>> nodeInstanceId : instanceInformations.entrySet()) {

            for (Map.Entry<String, InstanceInformation> nodeInstanceNumber : nodeInstanceId.getValue().entrySet()) {

                if (nodeInstanceNumber.getValue().getAttributes() != null) {
                    for (Map.Entry<String, String> attributeEntry : nodeInstanceNumber.getValue().getAttributes().entrySet()) {
                        PaaSNodeTemplate nodeTemplate = paaSTopology.getAllNodes().get(nodeInstanceId.getKey());
                        Map<String, IValue> nodeTemplateAttributes = nodeTemplate.getIndexedToscaElement().getAttributes();
                        IValue attributeValue = nodeTemplateAttributes.get(attributeEntry.getKey());
                        if (attributeValue != null) {
                            String parsedAttribute = FunctionEvaluator.parseAttribute(attributeEntry.getKey(), attributeValue, topology, instanceInformations,
                                    nodeInstanceNumber.getKey(), nodeTemplate, paaSTopology.getAllNodes());
                            attributeEntry.setValue(parsedAttribute);
                        }
                    }
                }
            }
        }
    }

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
    public static String parseAttribute(String attributeId, IValue attributeValue, Topology topology,
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
                if (!runtimeAttributeValue.contains("=Error!]") && !runtimeAttributeValue.equals("")) {
                    return runtimeAttributeValue;
                }
            }

            return ((AttributeDefinition) attributeValue).getDefault();
        }

        // handle concat function
        if (attributeValue instanceof ConcatPropertyValue) {
            StringBuilder evaluatedAttribute = new StringBuilder();
            ConcatPropertyValue concatPropertyValue = (ConcatPropertyValue) attributeValue;
            for (IValue concatParam : concatPropertyValue.getParameters()) {
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
                    FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) concatParam;
                    List<? extends IPaaSTemplate> paasTemplates = getPaaSTemplatesFromKeyword(basePaaSTemplate, functionPropertyValue.getTemplateName(),
                            builtPaaSTemplates);
                    switch (functionPropertyValue.getFunction()) {
                    case ToscaFunctionConstants.GET_ATTRIBUTE:
                        evaluatedAttribute.append(extractRuntimeInformationAttribute(runtimeInformations, currentInstance, paasTemplates,
                                functionPropertyValue.getElementNameToFetch()));
                        break;
                    case ToscaFunctionConstants.GET_PROPERTY:
                        evaluatedAttribute.append(extractRuntimeInformationProperty(topology, functionPropertyValue.getElementNameToFetch(), paasTemplates));
                        break;
                    case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
                        String defaultValue = "<" + functionPropertyValue.getElementNameToFetch() + ">";
                        evaluatedAttribute.append(extractRuntimeInformationOperationOutput(runtimeInformations, currentInstance, paasTemplates,
                                functionPropertyValue, defaultValue));
                        break;
                    default:
                        log.warn("Function [{}] is not yet handled in concat operation.", functionPropertyValue.getFunction());
                        break;
                    }

                }
            }
            return evaluatedAttribute.toString();
        }

        // handle functions. For now, only support Get_OPERATION_OUTPUT on attributes scope
        if (attributeValue instanceof FunctionPropertyValue) {
            FunctionPropertyValue function = (FunctionPropertyValue) attributeValue;
            switch (function.getFunction()) {
            case ToscaFunctionConstants.GET_OPERATION_OUTPUT:
                List<? extends IPaaSTemplate> paasTemplates = getPaaSTemplatesFromKeyword(basePaaSTemplate, function.getTemplateName(), builtPaaSTemplates);
                return extractRuntimeInformationOperationOutput(runtimeInformations, currentInstance, paasTemplates, function, null);
            default:
                return null;
            }
        }

        return null;
    }

    private static String extractRuntimeInformationOperationOutput(Map<String, Map<String, InstanceInformation>> runtimeInformations, String instanceId,
            List<? extends IPaaSTemplate> nodes, FunctionPropertyValue function, String defaultValue) {
        String outputRQN = AlienUtils.prefixWith(AlienConstants.OPERATION_NAME_SEPARATOR, function.getElementNameToFetch(),
                new String[] { function.getInterfaceName(), function.getOperationName() });
        // return the first found
        for (IPaaSTemplate node : nodes) {
            String nodeName = node.getId();
            if (runtimeInformations.get(nodeName) != null) {
                Map<String, String> outputs;
                // get value for an instance if instance number found
                if (runtimeInformations.get(nodeName).containsKey(instanceId)) {
                    outputs = runtimeInformations.get(nodeName).get(instanceId).getOperationsOutputs();
                } else {
                    outputs = runtimeInformations.get(nodeName).entrySet().iterator().next().getValue().getAttributes();
                }
                String formatedOutputName = ToscaUtils.formatedOperationOutputName(nodeName, function.getInterfaceName(), function.getOperationName(),
                        function.getElementNameToFetch());
                if (outputs.containsKey(formatedOutputName)) {
                    return outputs.get(formatedOutputName);
                }
            }
        }
        log.warn("Couldn't find output <{}> in nodes <{}>", outputRQN, nodes.toString());
        return defaultValue;
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
        case ToscaFunctionConstants.SELF:
            return Lists.<IPaaSTemplate> newArrayList(basePaaSTemplate);
        case ToscaFunctionConstants.HOST:
            return getWithParentsNodes(getHostNode(basePaaSTemplate));
        case ToscaFunctionConstants.SOURCE:
            return getWithParentsNodes(getSourceNode(basePaaSTemplate, builtPaaSTemplates));
        case ToscaFunctionConstants.TARGET:
            return getWithParentsNodes(getTargetNode(basePaaSTemplate, builtPaaSTemplates));
        default:
            // FIXME if the keyword is in fact the name of a relationship??
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
    public static String evaluateGetPropertyFunction(FunctionPropertyValue functionParam,
            IPaaSTemplate<? extends IndexedInheritableToscaElement> basePaaSTemplate, Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        List<? extends IPaaSTemplate> paaSTemplates = getPaaSTemplatesFromKeyword(basePaaSTemplate, functionParam.getTemplateName(), builtPaaSTemplates);
        for (IPaaSTemplate paaSTemplate : paaSTemplates) {
            String propertyValue = getPropertyFromTemplateOrCapability(paaSTemplate, functionParam.getCapabilityOrRequirementName(),
                    functionParam.getElementNameToFetch());
            // return the first value found
            if (propertyValue != null) {
                return propertyValue;
            }
        }
        return null;
    }

    private static String serializeComplexPropertyValue(Object value) {
        try {
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof ComplexPropertyValue) {
                return JsonUtil.toString(((ComplexPropertyValue) value).getValue());
            } else {
                return JsonUtil.toString(value);
            }
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String getPropertyValue(Map<String, AbstractPropertyValue> properties, Map<String, PropertyDefinition> propertyDefinitions,
            String propertyAccessPath) {
        if (properties == null || !properties.containsKey(propertyAccessPath)) {
            String propertyName = PropertyUtil.getPropertyNameFromComplexPath(propertyAccessPath);
            if (propertyName == null) {
                // Non complex
                String defaultValue = PropertyUtil.getDefaultValueFromPropertyDefinitions(propertyAccessPath, propertyDefinitions);
                if (defaultValue != null) {
                    return defaultValue;
                } else {
                    return null;
                }
            } else {
                // Complex
                PropertyDefinition propertyDefinition = propertyDefinitions.get(propertyName);
                if (propertyDefinition == null) {
                    return null;
                } else if (ToscaType.isSimple(propertyDefinition.getType())) {
                    // It's a complex path (with '.') but the type in definition is finally simple
                    return null;
                } else if (properties != null) {
                    AbstractPropertyValue rawValue = properties.get(propertyName);
                    if (!(rawValue instanceof PropertyValue)) {
                        throw new NotSupportedException("Only support static value in a get_property");
                    }
                    Object value = MapUtil.get(((PropertyValue) rawValue).getValue(), propertyAccessPath.substring(propertyName.length() + 1));
                    return serializeComplexPropertyValue(value);
                } else {
                    return null;
                }
            }
        } else {
            AbstractPropertyValue abstractPropertyValue = properties.get(propertyAccessPath);
            if (abstractPropertyValue == null) {
                return null;
            } else if (!(abstractPropertyValue instanceof PropertyValue)) {
                throw new NotSupportedException("Not a property value " + abstractPropertyValue);
            } else if (abstractPropertyValue instanceof ScalarPropertyValue) {
                return getScalarValue(properties.get(propertyAccessPath));
            } else {
                try {
                    return JsonUtil.toString(((PropertyValue) abstractPropertyValue).getValue());
                } catch (JsonProcessingException e) {
                    return null;
                }
            }
        }
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
    private static String getPropertyFromTemplateOrCapability(IPaaSTemplate<? extends IndexedInheritableToscaElement> paaSTemplate,
            String capabilityOrRequirementName, String elementName) {

        // if no capability or requirement provided, return the value from the template property
        if (StringUtils.isBlank(capabilityOrRequirementName)) {
            return getPropertyValue(paaSTemplate.getTemplate().getProperties(), paaSTemplate.getIndexedToscaElement().getProperties(), elementName);
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

            if (propertyValue instanceof ComplexPropertyValue) {
                return serializeComplexPropertyValue(((ComplexPropertyValue) propertyValue).getValue());
            } else {
                return getScalarValue(propertyValue);
            }
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

    private static PaaSNodeTemplate getHostNode(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (basePaaSTemplate instanceof PaaSNodeTemplate) {
            // TODO Must review this management of host
            PaaSNodeTemplate template = (PaaSNodeTemplate) basePaaSTemplate;
            return template.getParent() != null ? template.getParent() : template;
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.HOST + "> can only be used on a NodeTemplate level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static PaaSNodeTemplate getSourceNode(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return getPaaSNodeOrFail(((PaaSRelationshipTemplate) basePaaSTemplate).getSource(), builtPaaSTemplates);
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.SOURCE + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static PaaSNodeTemplate getTargetNode(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return getPaaSNodeOrFail(((PaaSRelationshipTemplate) basePaaSTemplate).getRelationshipTemplate().getTarget(), builtPaaSTemplates);
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.TARGET + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">.");
    }

    private static List<PaaSNodeTemplate> getWithParentsNodes(final PaaSNodeTemplate paaSNodeTemplate) {
        List<PaaSNodeTemplate> toReturn = Lists.newArrayList();
        PaaSNodeTemplate parent = paaSNodeTemplate;
        while (parent != null) {
            toReturn.add(parent);
            parent = parent.getParent();
        }
        return toReturn;
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

    public static boolean isGetOperationOutput(FunctionPropertyValue function) {
        return ToscaFunctionConstants.GET_OPERATION_OUTPUT.equals(function.getFunction());
    }

    /**
     * Check if the given property value is a TOSCA get_input function.
     *
     * @param propertyValue The property value to evaluate.
     * @return True if the property is a TOSCA get_input function, false if not.
     */
    public static boolean isGetInput(AbstractPropertyValue propertyValue) {
        return propertyValue instanceof FunctionPropertyValue && isGetInput((FunctionPropertyValue) propertyValue);
    }

    public static boolean isGetInput(FunctionPropertyValue function) {
        return ToscaFunctionConstants.GET_INPUT.equals(function.getFunction());
    }
}