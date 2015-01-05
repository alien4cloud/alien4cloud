package alien4cloud.tosca.container;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.InstanceInformation;

/**
 * Utility class to process functions
 *
 * @author luc boutier
 */
@Slf4j
public final class ToscaFunctionProcessor {
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
    public static String parseString(String str, Topology topology, Map<String, Map<Integer, InstanceInformation>> runtimeInformations, int currentInstance) {
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
                String propertyValue = template.getProperties().get(propertyName);
                if (propertyValue != null) {
                    sb.append(topology.getNodeTemplates().get(nodeName).getProperties().get(propertyName));
                }
            }
        }
        sb.append(str.substring(cursor));
        return sb.toString();
    }

    public static String parseAttributes(String str, Map<String, Map<Integer, InstanceInformation>> runtimeInformations, int currentInstance) {
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
                if (runtimeInformations.get(nodeName).size() > currentInstance) {
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
}