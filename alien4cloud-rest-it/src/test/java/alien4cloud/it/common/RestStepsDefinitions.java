package alien4cloud.it.common;

import static alien4cloud.it.Context.getRestClientInstance;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.google.common.collect.Maps;

import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;

/**
 * Simple steps definitions for get/post/put/patch requests.
 */
public class RestStepsDefinitions {
    Pattern p = Pattern.compile("\\{([\\w\\.]+)\\}");

    // Map to keep track of some elements between queries (ids, results etc.)
    public static final Map<String, Object> REGISTRY = Maps.newHashMap();

    @When("^I POST \"(.*?)\" to \"(.*?)\"$")
    public void post(String jsonFilePath, String path) throws Throwable {
        String processedPath = processPathForRegistryKeys(path);
        String postBody = null;
        if (jsonFilePath != null) {
            postBody = FileUtil.readTextFile(Context.REQUESTS_PATH.resolve(jsonFilePath));
        }
        Context.getInstance().registerRestResponse(getRestClientInstance().postJSon(processedPath, postBody));
    }

    @When("^I GET \"(.*?)\"$")
    public void get(String path) throws Throwable {
        String processedPath = processPathForRegistryKeys(path);
        Context.getInstance().registerRestResponse(getRestClientInstance().get(processedPath));
    }

    @When("^I PUT \"(.*?)\" to \"(.*?)\"$")
    public void put(String jsonFilePath, String path) throws Throwable {
        String processedPath = processPathForRegistryKeys(path);
        String postBody = null;
        if (jsonFilePath != null) {
            postBody = FileUtil.readTextFile(Context.REQUESTS_PATH.resolve(jsonFilePath));
        }
        Context.getInstance().registerRestResponse(getRestClientInstance().putJSon(processedPath, postBody));
    }

    @When("^I PATCH \"(.*?)\" to \"(.*?)\"$")
    public void patch(String jsonFilePath, String path) throws Throwable {
        String processedPath = processPathForRegistryKeys(path);
        String postBody = null;
        if (jsonFilePath != null) {
            postBody = FileUtil.readTextFile(Context.REQUESTS_PATH.resolve(jsonFilePath));
        }
        Context.getInstance().registerRestResponse(getRestClientInstance().patchJSon(processedPath, postBody));
    }

    @When("^I DELETE \"(.*?)\"$")
    public void delete(String path) throws Throwable {
        String processedPath = processPathForRegistryKeys(path);
        Context.getInstance().registerRestResponse(getRestClientInstance().delete(processedPath));
    }

    @And("^I register \"(.*?)\" as \"(.*?)\"$")
    public void register(String propertyPath, String key) throws Throwable {
        RestResponse restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Object value = restResponse;
        if (!"null".equals(propertyPath)) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(restResponse);
            value = beanWrapper.getPropertyValue(propertyPath);
        }
        REGISTRY.put(key, value);
    }

    @And("^I register path \"(.*?)\" with class \"(.*?)\" as \"(.*?)\"$")
    public void deserAndRegister(String propertyPath, String className, String key) throws Throwable {
        Class clazz = Class.forName(className);
        RestResponse restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), clazz);
        Object value = restResponse;
        if (!"null".equals(propertyPath)) {
            BeanWrapper beanWrapper = new BeanWrapperImpl(restResponse);
            value = beanWrapper.getPropertyValue(propertyPath);
        }
        REGISTRY.put(key, value);
    }

    @And("^I register \"(.*?)\" for SPEL$")
    public void registerForSPEL(String key) throws Throwable {
        Context.getInstance().buildEvaluationContext(REGISTRY.get(key));
    }

    private String processPathForRegistryKeys(String path) {
        Matcher m = p.matcher(path);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            m.appendReplacement(result, (String) REGISTRY.get(key));
        }
        m.appendTail(result);
        return result.toString();
    }
}