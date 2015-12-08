package alien4cloud.it.provider.util;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Maps;

public class AttributeUtil {

    public static Map<String, String> getAttributes(String nodeName, String attributeName) throws IOException {
        Map<String, String> nodeAttributes = Maps.newHashMap();
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
            Map<String, Object> instanceAttributes = (Map<String, Object>) instanceInformation.get("attributes");
            nodeAttributes.put(instanceInformationEntry.getKey(), (String) instanceAttributes.get(attributeName));
        }
        return nodeAttributes;
    }

    public static String getAttribute(String nodeName, String attributeName) throws IOException {
        return getAttribute(nodeName, attributeName, 0);
    }

    public static String getAttribute(String nodeName, String attributeName, int instanceIdx) throws IOException {
        Map<String, String> allInstancesAttributes = getAttributes(nodeName, attributeName);
        Assert.assertTrue(MapUtils.isNotEmpty(allInstancesAttributes));
        Assert.assertTrue(allInstancesAttributes.size() >= instanceIdx + 1);
        Iterator<String> it = allInstancesAttributes.values().iterator();
        for (int i = 0; i < instanceIdx; i++) {
            it.next();
        }
        return it.next();
    }
}
