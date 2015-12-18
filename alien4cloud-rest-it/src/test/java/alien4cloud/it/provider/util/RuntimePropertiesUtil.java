package alien4cloud.it.provider.util;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Maps;

public class RuntimePropertiesUtil {

    public static Map<String, String> getProperties(String nodeName, String propertyName) throws IOException {
        Map<String, String> nodeRuntimeProperties = Maps.newHashMap();
        RestResponse<?> response = JsonUtil.read(Context.getRestClientInstance().get(
                "/rest/applications/" + ApplicationStepDefinitions.CURRENT_APPLICATION.getId() + "/environments/"
                        + Context.getInstance().getDefaultApplicationEnvironmentId(ApplicationStepDefinitions.CURRENT_APPLICATION.getName())
                        + "/deployment/informations"));
        Map<String, Object> instancesInformation = (Map<String, Object>) response.getData();
        Assert.assertFalse(MapUtils.isEmpty(instancesInformation));
        Map<String, Object> nodeInformation = (Map<String, Object>) instancesInformation.get(nodeName);
        Assert.assertFalse(MapUtils.isEmpty(nodeInformation));
        for (Map.Entry<String, Object> instanceInformationEntry : nodeInformation.entrySet()) {
            Map<String, Object> instanceInformation = (Map<String, Object>) instanceInformationEntry.getValue();
            Map<String, Object> instanceRuntimeProperties = (Map<String, Object>) instanceInformation.get("runtimeProperties");
            nodeRuntimeProperties.put(instanceInformationEntry.getKey(), (String) instanceRuntimeProperties.get(propertyName));
        }
        return nodeRuntimeProperties;
    }

    public static String getProperty(String nodeName, String propertyName) throws IOException {
        Map<String, String> allInstancesRuntimeProperties = getProperties(nodeName, propertyName);
        Assert.assertTrue(MapUtils.isNotEmpty(allInstancesRuntimeProperties));
        Assert.assertTrue(allInstancesRuntimeProperties.size() == 1);
        return allInstancesRuntimeProperties.values().iterator().next();
    }
}
