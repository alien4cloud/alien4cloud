package alien4cloud.it.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.apache.commons.collections.MapUtils;

import alien4cloud.rest.utils.JsonUtil;

/**
 *  It is a util class for dealing with the property
 */
public class PropertyUtils {

    /**
     * Check if a property json string is a function
     * @param functionName
     * @param jsonProperty
     * @return
     * @throws IOException
     */
    public static boolean isFunction(String functionName, String jsonProperty) throws IOException {
        Map<String, Object> propertyMap = JsonUtil.toMap(jsonProperty);
        if (MapUtils.isEmpty(propertyMap)) {
            return false;
        }
        if (propertyMap.size() == 2 && propertyMap.containsKey("function") && propertyMap.containsKey("parameters")) {
            if (functionName.equals(propertyMap.get("function"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Transform a json property string to a FunctionProperty object
     * @param jsonProperty
     * @return
     */
    public static FunctionPropertyValue toFunctionValue(String jsonProperty) throws IOException {
        Map<String, Object> propertyMap = JsonUtil.toMap(jsonProperty);
        FunctionPropertyValue propertyValue = new FunctionPropertyValue();
        propertyValue.setFunction(propertyMap.get("function").toString());
        propertyValue.setParameters((List<String>) propertyMap.get("parameters"));
        return propertyValue;
    }
}
