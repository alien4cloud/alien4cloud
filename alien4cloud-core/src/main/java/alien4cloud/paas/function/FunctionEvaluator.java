package alien4cloud.paas.function;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.AttributeDefinition;
import alien4cloud.model.components.ConcatPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IAttributeValue;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.NodeTemplate;
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
     * @return
     */
    public static String parseAttribute(String attributeId, IAttributeValue attributeValue, Topology topology,
            Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance,
            IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {

        if (attributeValue == null) {
            return null;
        }

        // handle AttributeDefinition type
        if (attributeValue instanceof AttributeDefinition) {
            String runtimeAttributeValue = extractRuntimeInformationAttribute(runtimeInformations, currentInstance, new String[] { basePaaSTemplate.getId() },
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
                String[] nodeNames = null;
                String propertyOrAttributeName = null;
                FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) concatParam;
                List<String> parameters = functionPropertyValue.getParameters();
                propertyOrAttributeName = parameters.get(1);
                nodeNames = evaluateEntityName(parameters.get(0), basePaaSTemplate);
                switch (functionPropertyValue.getFunction()) {
                case ToscaFunctionConstants.GET_ATTRIBUTE:
                    evaluatedAttribute.append(extractRuntimeInformationAttribute(runtimeInformations, currentInstance, nodeNames, propertyOrAttributeName));
                    break;
                case ToscaFunctionConstants.GET_PROPERTY:
                    evaluatedAttribute.append(extractRuntimeInformationProperty(topology, propertyOrAttributeName, nodeNames));
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
     * @param nodeNames
     * @return
     */
    private static String extractRuntimeInformationProperty(Topology topology, String propertyOrAttributeName, String[] nodeNames) {
        AbstractPropertyValue propertyOrAttributeValue;
        NodeTemplate template = null;
        for (String nodeName : nodeNames) {
            template = topology.getNodeTemplates().get(nodeName);
            if (template != null && template.getProperties() != null) {
                propertyOrAttributeValue = template.getProperties().get(propertyOrAttributeName);
                if (propertyOrAttributeValue != null) {
                    return getScalarValue(propertyOrAttributeValue);
                }
            }
        }
        log.warn("Couldn't find property <{}> of node <{}>", propertyOrAttributeName, nodeNames);
        return "[" + nodeNames + "." + propertyOrAttributeName + "=Error!]";
    }

    /**
     * Return the first matching value in parent nodes hierarchy
     * 
     * @param runtimeInformations
     * @param currentInstance
     * @param nodeName
     * @param propertyOrAttributeName
     * @return runtime value
     */
    private static String extractRuntimeInformationAttribute(Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance,
            String[] nodeNames, String propertyOrAttributeName) {
        Map<String, String> attributes = null;
        // return the first found
        for (String nodeName : nodeNames) {
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
        log.warn("Couldn't find attribute <{}> in nodes <{}>", propertyOrAttributeName, nodeNames.toString());
        return "<" + propertyOrAttributeName + ">"; // value not yet computed (or won't be computes)
    }

    /**
     * Return the paaS entity based on a keyword. This latest can be a special keyword (SELF, SOURCE, TARGET, ...), or a node template name
     *
     * @param basePaaSTemplate The base PaaSTemplate for which to get the entity name
     * @param keyword The
     * @param builtPaaSTemplates
     * @return the PaaSNodeTemplate resulting from the evaluation
     */
    public static List<PaaSNodeTemplate> getPaaSEntities(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate, String keyword,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        String[] entityNameList = evaluateEntityName(keyword, basePaaSTemplate);
        List<PaaSNodeTemplate> templateList = Lists.newArrayList();
        for (String entity : entityNameList) {
            templateList.add(getPaaSNodeOrDie(entity, builtPaaSTemplates));
        }
        return templateList;
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
        List<PaaSNodeTemplate> entities = getPaaSEntities(basePaaSTemplate, functionParam.getParameters().get(0), builtPaaSTemplates);
        String propertyId = functionParam.getParameters().get(1);
        for (PaaSNodeTemplate paaSNodeTemplate : entities) {
            // the first nodeTemplate with the required propertyId is returned
            if (paaSNodeTemplate.getNodeTemplate().getProperties().containsKey(propertyId)) {
                AbstractPropertyValue propertyValue = paaSNodeTemplate.getNodeTemplate().getProperties().get(propertyId);
                if (propertyValue instanceof ScalarPropertyValue) {
                    return ((ScalarPropertyValue) propertyValue).getValue();
                } else if (propertyValue instanceof FunctionPropertyValue) {
                    return evaluateGetPropertyFuntion((FunctionPropertyValue) propertyValue, paaSNodeTemplate, builtPaaSTemplates);
                } else {
                    throw new FunctionEvaluationException("");
                }
            }
        }
        return null;
    }

    private static PaaSNodeTemplate getPaaSNodeOrDie(String nodeId, Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        PaaSNodeTemplate toReturn = builtPaaSTemplates.get(nodeId);
        if (toReturn == null) {
            throw new FunctionEvaluationException(" Failled to retrieve the nodeTemplate with name <" + nodeId + ">");
        }
        return toReturn;
    }

    private static String[] evaluateEntityName(String stringToEval, IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        switch (stringToEval) {
        case ToscaFunctionConstants.HOST:
            return getHostNodeId(basePaaSTemplate);
        case ToscaFunctionConstants.SELF:
            return new String[] { getSelfNodeId(basePaaSTemplate) };
        case ToscaFunctionConstants.SOURCE:
            return new String[] { getSourceNodeId(basePaaSTemplate) };
        case ToscaFunctionConstants.TARGET:
            return new String[] { getTargetNodeId(basePaaSTemplate) };
        default:
            return new String[] { stringToEval };
        }
    }

    private static String getSelfNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (basePaaSTemplate instanceof PaaSNodeTemplate) {
            return basePaaSTemplate.getId();
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.SELF + "> can only be used on a NodeTemplate level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static String[] getHostNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (!(basePaaSTemplate instanceof PaaSNodeTemplate)) {
            throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.HOST + "> can only be used on a NodeTemplate level's parameter. Node<"
                    + basePaaSTemplate.getId() + ">");
        }
        try {
            List<PaaSNodeTemplate> parentList = ToscaUtils.getParents((PaaSNodeTemplate) basePaaSTemplate);
            List<String> parentIdsList = Lists.newArrayList();
            for (PaaSNodeTemplate template : parentList) {
                parentIdsList.add(template.getId());
            }
            return parentIdsList.toArray(new String[parentIdsList.size()]);
        } catch (PaaSTechnicalException e) {
            throw new FunctionEvaluationException("Failed to retrieve the root node of <" + basePaaSTemplate.getId() + ">.", e);
        }
    }

    private static String getSourceNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return ((PaaSRelationshipTemplate) basePaaSTemplate).getSource();
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.SOURCE + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static String getTargetNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (basePaaSTemplate instanceof PaaSRelationshipTemplate) {
            return ((PaaSRelationshipTemplate) basePaaSTemplate).getRelationshipTemplate().getTarget();
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.TARGET + "> can only be used on a Relationship level's parameter. Node<"
                + basePaaSTemplate.getId() + ">.");
    }

    public static String getEntityName(FunctionPropertyValue function) {
        return function.getParameters().get(0);
    }

    public static String getElementName(FunctionPropertyValue function) {
        return function.getParameters().get(1);
    }

    public static boolean isGetAttribute(FunctionPropertyValue function) {
        return ToscaFunctionConstants.GET_ATTRIBUTE.equals(function.getFunction());
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
        Map<String, String> properties = Maps.newHashMap();
        for (Map.Entry<String, AbstractPropertyValue> propertyValueEntry : propertyValues.entrySet()) {
            properties.put(propertyValueEntry.getKey(), getScalarValue(propertyValueEntry.getValue()));
        }
        return properties;
    }
}