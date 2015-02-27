package alien4cloud.paas.function;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.FunctionPropertyValue;
import alien4cloud.model.components.IndexedToscaElement;
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

import com.google.common.collect.Maps;

/**
 * Utility class to process functions defined in attributes level:
 * ex:
 * attributes:
 * url: "http://get_property: [the_node_tempalte_1, the_property_name_1]:get_property: [the_node_tempalte_2, the_property_name_2 ]/super"
 */
@Slf4j
public final class FunctionEvaluator {
    private static Pattern getPropertyPattern = Pattern.compile("get_property\\s*:\\s*\\[\\s*(\\w*)\\s*,\\s*(\\w*)\\s*\\]");
    private static Pattern getAttributePattern = Pattern.compile("get_attribute\\s*:\\s*\\[\\s*(\\w*)\\s*,\\s*(\\w*)\\s*\\]");

    /**
     * Parse a string to render the attribute or property based on topology and runtime data.
     *
     * @param str The attribute (or property) string.
     * @param topology The topology that holds properties definitions.
     * @param runtimeInformations The runtime informations (that should holds attribute informations).
     * @param currentInstance The instance id of the current node for which to parse the attribute or property string (str).
     * @return A string with complete informations.
     */
    public static String parseString(String str, Topology topology, Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance) {
        String parsedString = parseProperties(str, topology);
        return parseAttributes(parsedString, runtimeInformations, currentInstance);
    }

    public static String parseProperties(String str, Topology topology) {
        if (str == null) {
            return str;
        }
        Matcher matcher = getPropertyPattern.matcher(str);
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            String nodeName = matcher.group(1);
            String propertyName = matcher.group(2);
            // get the actual value for the property.
            sb.append(str.substring(cursor, matcher.start()));
            cursor = matcher.end();
            NodeTemplate template = topology.getNodeTemplates().get(nodeName);
            if (template != null) {
                String propertyValue = null;
                AbstractPropertyValue abstractPropertyValue = template.getProperties().get(propertyName);
                if (abstractPropertyValue instanceof ScalarPropertyValue) {
                    propertyValue = ((ScalarPropertyValue) abstractPropertyValue).getValue();
                }
                if (propertyValue != null) {
                    sb.append(propertyValue);
                }
            }
        }
        sb.append(str.substring(cursor));
        return sb.toString();
    }

    public static String parseAttributes(String str, Map<String, Map<String, InstanceInformation>> runtimeInformations, String currentInstance) {
        if (str == null) {
            return str;
        }
        Matcher matcher = getAttributePattern.matcher(str);
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            String nodeName = matcher.group(1);
            String attributeName = matcher.group(2);
            sb.append(str.substring(cursor, matcher.start()));
            cursor = matcher.end();
            String attributeValue;
            if (runtimeInformations.get(nodeName) != null) {
                if (runtimeInformations.get(nodeName).containsKey(currentInstance)) {
                    attributeValue = runtimeInformations.get(nodeName).get(currentInstance).getAttributes().get(attributeName);
                } else {
                    attributeValue = runtimeInformations.get(nodeName).entrySet().iterator().next().getValue().getAttributes().get(attributeName);
                }
                sb.append(attributeValue);
            } else {
                log.warn("Couldn't find attributes/properties of in node <{}>", nodeName);
                sb.append("[" + nodeName + "." + attributeName + "=Error!]");
            }
        }
        sb.append(str.substring(cursor));

        return sb.toString();
    }

    /**
     * Return the paaS entity based on a keyword. This latest can be a special keyword (SELF, SOURCE, TARGET, ...), or a node template name
     *
     * @param basePaaSTemplate The base PaaSTemplate for which to get the entity name
     * @param keyword The
     * @param builtPaaSTemplates
     * @return the PaaSNodeTemplate resulting from the evaluation
     */
    public static PaaSNodeTemplate getPaaSEntity(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate, String keyword,
            Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        String entityName = evaluateEntityName(keyword, basePaaSTemplate);
        // TODO: handle the case basePaaSTemplate is a paaSRelationshipTemplate
        // TODO: handle the case params size greater than 2. That means we have to retrieve the property on a requirement or a capability
        PaaSNodeTemplate entity = getPaaSNodeOrDie(entityName, builtPaaSTemplates);
        return entity;
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
        PaaSNodeTemplate entity = getPaaSEntity(basePaaSTemplate, functionParam.getParameters().get(0), builtPaaSTemplates);
        String result = null;
        if (entity.getNodeTemplate().getProperties() != null) {
            AbstractPropertyValue abstractPropertyValue = entity.getNodeTemplate().getProperties().get(functionParam.getParameters().get(1));
            result = (abstractPropertyValue instanceof ScalarPropertyValue) ? ((ScalarPropertyValue) abstractPropertyValue).getValue() : null;
        }
        return result;
    }

    private static PaaSNodeTemplate getPaaSNodeOrDie(String nodeId, Map<String, PaaSNodeTemplate> builtPaaSTemplates) {
        PaaSNodeTemplate toReturn = builtPaaSTemplates.get(nodeId);
        if (toReturn == null) {
            throw new FunctionEvaluationException(" Failled to retrieve the nodeTemplate with name <" + nodeId + ">");
        }
        return toReturn;
    }

    private static String evaluateEntityName(String stringToEval, IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        switch (stringToEval) {
        case ToscaFunctionConstants.HOST:
            return getHostNodeId(basePaaSTemplate);
        case ToscaFunctionConstants.SELF:
            return getSelfNodeId(basePaaSTemplate);
        case ToscaFunctionConstants.SOURCE:
            return getSourceNodeId(basePaaSTemplate);
        case ToscaFunctionConstants.TARGET:
            return getTargetNodeId(basePaaSTemplate);
        default:
            return stringToEval;
        }
    }

    private static String getSelfNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (basePaaSTemplate instanceof PaaSNodeTemplate) {
            return basePaaSTemplate.getId();
        }
        throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.SELF + "> can only be used on a NodeTemplate level's parameter. Node<"
                + basePaaSTemplate.getId() + ">");
    }

    private static String getHostNodeId(IPaaSTemplate<? extends IndexedToscaElement> basePaaSTemplate) {
        if (!(basePaaSTemplate instanceof PaaSNodeTemplate)) {
            throw new BadUsageKeywordException("The keyword <" + ToscaFunctionConstants.HOST + "> can only be used on a NodeTemplate level's parameter. Node<"
                    + basePaaSTemplate.getId() + ">");
        }
        try {
            return ToscaUtils.getHostTemplate((PaaSNodeTemplate) basePaaSTemplate).getId();
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