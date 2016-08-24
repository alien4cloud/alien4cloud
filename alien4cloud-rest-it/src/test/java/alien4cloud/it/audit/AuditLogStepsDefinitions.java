package alien4cloud.it.audit;

import java.io.IOException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;

import alien4cloud.audit.model.AuditTrace;
import alien4cloud.audit.model.AuditedMethod;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.it.Context;
import alien4cloud.audit.rest.AuditConfigurationDTO;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.utils.JsonUtil;

import com.google.common.collect.Lists;

import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class AuditLogStepsDefinitions {

    private AuditConfigurationDTO currentAuditConfiguration = null;

    @Then("^I should have no audit trace in Alien$")
    public void I_should_have_no_audit_trace_in_Alien() throws Throwable {
        SearchRequest req = new SearchRequest(null, "", 0, 1, null);
        String jSon = JsonUtil.toString(req);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/v1/audit/search", jSon);
        FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
        Assert.assertEquals(0, searchResult.getTotalResults());
    }

    @Then("^I should have (\\d+) audit traces in Alien:$")
    public void I_should_have_audit_traces_in_Alien(int numberOfResult, DataTable rawExpectedAuditTraces) throws Throwable {
        SearchRequest req = new SearchRequest(null, "", 0, numberOfResult, null);
        String jSon = JsonUtil.toString(req);
        String restResponse = Context.getRestClientInstance().postJSon("/rest/v1/audit/search", jSon);
        FacetedSearchResult searchResult = JsonUtil.read(restResponse, FacetedSearchResult.class).getData();
        Assert.assertEquals(numberOfResult, searchResult.getTotalResults());
        Object[] searchData = searchResult.getData();
        List<AuditTrace> actualTraces = Lists.newArrayList();
        for (int i = 0; i < searchData.length; i++) {
            actualTraces.add(JsonUtil.readObject(JsonUtil.toString(searchData[i]), AuditTrace.class));
        }
        for (List<String> row : rawExpectedAuditTraces.raw()) {
            String userName = row.get(0);
            String category = row.get(1);
            String action = row.get(2);
            Assert.assertTrue(auditContains(actualTraces, userName, category, action));
        }
    }

    private boolean auditContains(List<AuditTrace> traces, String userName, String category, String action) {
        for (AuditTrace trace : traces) {
            boolean userMatch = trace.getUserName().equals(userName);
            boolean categoryMatch = trace.getCategory().equals(category);
            boolean actionMatch = trace.getAction().equals(action);
            if (userMatch && categoryMatch && actionMatch) {
                return true;
            }
        }
        return false;
    }

    @When("^I get audit log configuration$")
    public void I_get_audit_log_configuration() throws Throwable {
        String restResponse = Context.getRestClientInstance().get("/rest/v1/audit/configuration");
        AuditConfigurationDTO configuration = JsonUtil.read(restResponse, AuditConfigurationDTO.class).getData();
        Assert.assertNotNull(configuration);
        currentAuditConfiguration = configuration;
    }

    @Then("^I should have audit log enabled globally$")
    public void I_should_have_audit_log_enabled_globally() throws Throwable {
        Assert.assertNotNull(currentAuditConfiguration);
        Assert.assertTrue(currentAuditConfiguration.isEnabled());
    }

    @Then("^I should have audit log disabled globally$")
    public void I_should_have_audit_log_disabled_globally() throws Throwable {
        Assert.assertNotNull(currentAuditConfiguration);
        Assert.assertFalse(currentAuditConfiguration.isEnabled());
    }

    @And("^I should have audit log enabled for:$")
    public void I_should_have_audit_log_enabled_for(DataTable rawExpectedMethods) throws Throwable {
        checkAuditLogEnabled(rawExpectedMethods, true);
    }

    @Then("^I should have audit log disabled for:$")
    public void I_should_have_audit_log_disabled_for(DataTable rawExpectedMethods) throws Throwable {
        checkAuditLogEnabled(rawExpectedMethods, false);
    }

    private void checkAuditLogEnabled(DataTable rawExpectedMethods, boolean enabled) {
        Assert.assertNotNull(currentAuditConfiguration);
        for (List<String> row : rawExpectedMethods.raw()) {
            String category = row.get(0);
            String action = row.get(1);
            Assert.assertTrue(methodFound(currentAuditConfiguration.getMethodsConfiguration().get(category), action, enabled));
        }
    }

    private boolean methodFound(List<AuditedMethod> methods, String actionName, boolean isEnabled) {
        return getMethod(methods, actionName, isEnabled) != null;
    }

    private AuditedMethod getMethod(List<AuditedMethod> methods, String actionName, boolean isEnabled) {
        for (AuditedMethod method : methods) {
            if (method.getAction().equals(actionName) && method.isEnabled() == isEnabled) {
                return method;
            }
        }
        return null;
    }

    private void enableAuditGlobally(Boolean enable) throws IOException {
        Context.getRestClientInstance().postUrlEncoded("/rest/v1/audit/configuration/enabled",
                Lists.<NameValuePair> newArrayList(new BasicNameValuePair("enabled", enable.toString())));
    }

    @When("^I enable audit log globally$")
    public void I_enable_audit_log_globally() throws Throwable {
        enableAuditGlobally(true);
    }

    @When("^I disable audit log globally$")
    public void I_disable_audit_log_globally() throws Throwable {
        enableAuditGlobally(false);
    }

    private void enableMethods(DataTable rawMethods, boolean enable) throws IOException {
        List<AuditedMethod> methodsToEnableDisable = Lists.newArrayList();
        for (List<String> row : rawMethods.raw()) {
            String category = row.get(0);
            String action = row.get(1);
            AuditedMethod method = getMethod(currentAuditConfiguration.getMethodsConfiguration().get(category), action, !enable);
            method.setEnabled(enable);
            Assert.assertNotNull(method);
            methodsToEnableDisable.add(method);
        }
        Context.getRestClientInstance().postJSon("/rest/v1/audit/configuration/audited-methods", JsonUtil.toString(methodsToEnableDisable));
    }

    @When("^I disable audit log for following methods:$")
    public void I_disable_audit_log_for_following_methods(DataTable rawMethods) throws Throwable {
        enableMethods(rawMethods, false);
    }

    @When("^I enable audit log for following methods:$")
    public void I_enable_audit_log_for_following_methods(DataTable rawMethods) throws Throwable {
        enableMethods(rawMethods, true);
    }

    @And("^I reset audit log configuration$")
    public void I_reset_audit_log_configuration() throws Throwable {
        String restResponse = Context.getRestClientInstance().postJSon("/rest/v1/audit/configuration/reset", "");
        AuditConfigurationDTO configuration = JsonUtil.read(restResponse, AuditConfigurationDTO.class).getData();
        Assert.assertNotNull(configuration);
        currentAuditConfiguration = configuration;
    }
}
