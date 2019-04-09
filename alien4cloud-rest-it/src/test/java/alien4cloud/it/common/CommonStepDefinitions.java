package alien4cloud.it.common;

import static org.alien4cloud.test.util.SPELUtils.evaluateAndAssertExpression;
import static org.alien4cloud.test.util.SPELUtils.evaluateAndAssertExpressionContains;

import java.nio.file.Files;
import java.util.List;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.exception.rest.FieldErrorDTO;
import org.alien4cloud.server.MaintenanceModeState;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.Client;
import com.google.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;

import alien4cloud.audit.AuditESDAO;
import alien4cloud.audit.model.*;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.application.ApplicationStepDefinitions;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.common.*;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.repository.Repository;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.model.PluginConfiguration;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonStepDefinitions {
    private final Client esClient = Context.getEsClientInstance();
    //private List<String> indicesToClean;

    public CommonStepDefinitions() {
/**************
        indicesToClean = Lists.newArrayList();
        indicesToClean.add(ApplicationEnvironment.class.getSimpleName().toLowerCase());
        indicesToClean.add(ApplicationVersion.class.getSimpleName().toLowerCase());
        indicesToClean.add(DeploymentInputs.class.getSimpleName().toLowerCase());
        indicesToClean.add(DeploymentMatchingConfiguration.class.getSimpleName().toLowerCase());
        indicesToClean.add(OrchestratorDeploymentProperties.class.getSimpleName().toLowerCase());
        indicesToClean.add(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
        indicesToClean.add(Application.class.getSimpleName().toLowerCase());
        indicesToClean.add(Orchestrator.class.getSimpleName().toLowerCase());
        indicesToClean.add(Location.class.getSimpleName().toLowerCase());
        indicesToClean.add(Csar.class.getSimpleName().toLowerCase());
        indicesToClean.add(Topology.class.getSimpleName().toLowerCase());
        indicesToClean.add(Deployment.class.getSimpleName().toLowerCase());
        indicesToClean.add(Group.class.getSimpleName().toLowerCase());
        indicesToClean.add(User.class.getSimpleName().toLowerCase());
        indicesToClean.add(MetaPropConfiguration.class.getSimpleName().toLowerCase());
        indicesToClean.add(CsarGitRepository.class.getSimpleName().toLowerCase());
        indicesToClean.add(PaaSDeploymentLog.class.getSimpleName().toLowerCase());
        indicesToClean.add(AuditESDAO.ALIEN_AUDIT_INDEX);
        indicesToClean.add(ElasticSearchDAO.SUGGESTION_INDEX);
        indicesToClean.add(Repository.class.getSimpleName().toLowerCase());
        indicesToClean.add(ServiceResource.class.getSimpleName().toLowerCase());
        indicesToClean.add(MaintenanceModeState.class.getSimpleName().toLowerCase());
        indicesToClean.add(Plugin.class.getSimpleName().toLowerCase());
        indicesToClean.add(PluginConfiguration.class.getSimpleName().toLowerCase());
*****************/
    }

    private boolean somethingFound(final SearchResponse searchResponse) {
        if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().getHits() == null
                || searchResponse.getHits().getHits().length == 0) {
            return false;
        }
        return true;
    }

    private void clearIndex(String indexName) {
       clearIndex (indexName, indexName);
    }

    private void clearIndex(String indexName, String typeName) {
        // get all elements and then use a bulk delete to remove data.
        SearchRequestBuilder searchRequestBuilder = esClient.prepareSearch(indexName).setTypes(typeName).setQuery(QueryBuilders.matchAllQuery())
                .setFetchSource(false);
        searchRequestBuilder.setFrom(0).setSize(1000);
        SearchResponse response = searchRequestBuilder.execute().actionGet();

        while (somethingFound(response)) {
            BulkRequestBuilder bulkRequestBuilder = esClient.prepareBulk().setRefreshPolicy(RefreshPolicy.IMMEDIATE);

            for (int i = 0; i < response.getHits().hits().length; i++) {
                String id = response.getHits().hits()[i].getId();
                bulkRequestBuilder.add(esClient.prepareDelete(indexName, typeName, id));
            }

            bulkRequestBuilder.execute().actionGet();

            if (response.getHits().totalHits() == response.getHits().hits().length) {
                response = null;
            } else {
                response = searchRequestBuilder.execute().actionGet();
            }
        }
    }

    @Before(value = "@reset", order = 1)
    public void beforeScenario() throws Throwable {
        // clear the edition cache
        // teardown the platform before removing all data
        // connect as admin
        AuthenticationStepDefinitions authenticationStepDefinitions = new AuthenticationStepDefinitions();
        authenticationStepDefinitions.I_am_authenticated_with_role("ADMIN");
        Context.getRestClientInstance().putUrlEncoded("/rest/v2/editor/clearCache",
                Lists.<NameValuePair> newArrayList(new BasicNameValuePair("force", "true")));
        Context.getRestClientInstance().postJSon("/rest/v1/maintenance/teardown-platform", "");

        if (log.isDebugEnabled()) {
            log.debug("Before scenario, clean up elastic search and alien repositories from {}", Context.getInstance().getAlienPath());
        }
        if (Files.exists(Context.getInstance().getTmpDirectory())) {
            log.debug("Removing temp directory [" + Context.getInstance().getTmpDirectory().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getTmpDirectory());
        }
        if (Files.exists(Context.getInstance().getRepositoryDirPath())) {
            log.debug("Removing repository directory [" + Context.getInstance().getRepositoryDirPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getRepositoryDirPath());
        }
        if (Files.exists(Context.getInstance().getUploadTempDirPath())) {
            log.debug("Removing upload temp directory [" + Context.getInstance().getUploadTempDirPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getUploadTempDirPath());
        }
        if (Files.exists(Context.getInstance().getPluginDirPath())) {
            log.debug("Removing plugin directory [" + Context.getInstance().getPluginDirPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getPluginDirPath());
        }
        if (Files.exists(Context.getInstance().getWorkPath())) {
            log.debug("Removing plugin directory [" + Context.getInstance().getWorkPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getWorkPath());
        }
        if (Files.exists(Context.getInstance().getArtifactDirPath())) {
            log.debug("Removing artifact directory [" + Context.getInstance().getArtifactDirPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getArtifactDirPath());
        }

        Files.createDirectories(Context.getInstance().getTmpDirectory());
        Files.createDirectories(Context.getInstance().getRepositoryDirPath());
        Files.createDirectories(Context.getInstance().getUploadTempDirPath());
        Files.createDirectories(Context.getInstance().getPluginDirPath());
        Files.createDirectories(Context.getInstance().getWorkPath().resolve("plugins/content"));
        Files.createDirectories(Context.getInstance().getWorkPath().resolve("plugins/ui"));
        Files.createDirectories(Context.getInstance().getArtifactDirPath());

        // Clean elastic search cluster
/*******************
        for (String index : indicesToClean) {
            //esClient.prepareDeleteByQuery(new String[] { index }).setQuery(QueryBuilders.matchAllQuery()).execute().get();
            //esClient.prepareDelete().setIndex(index).execute().get();
        }
********************/
        clearIndex(ApplicationEnvironment.class.getSimpleName().toLowerCase());
        clearIndex(ApplicationVersion.class.getSimpleName().toLowerCase());
        clearIndex(DeploymentInputs.class.getSimpleName().toLowerCase());
        clearIndex(DeploymentMatchingConfiguration.class.getSimpleName().toLowerCase());
        clearIndex(OrchestratorDeploymentProperties.class.getSimpleName().toLowerCase());

        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, CapabilityType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, ArtifactType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, RelationshipType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, NodeType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, DataType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, PrimitiveDataType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, PolicyType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, AbstractInstantiableToscaType.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.TOSCA_ELEMENT_INDEX, AbstractToscaType.class.getSimpleName().toLowerCase());

        clearIndex(Application.class.getSimpleName().toLowerCase());
        clearIndex(Orchestrator.class.getSimpleName().toLowerCase());
        clearIndex(Location.class.getSimpleName().toLowerCase());
        clearIndex(Csar.class.getSimpleName().toLowerCase());
        clearIndex(Topology.class.getSimpleName().toLowerCase());
        clearIndex(Deployment.class.getSimpleName().toLowerCase());
        clearIndex(Group.class.getSimpleName().toLowerCase());
        clearIndex(User.class.getSimpleName().toLowerCase());
        clearIndex(MetaPropConfiguration.class.getSimpleName().toLowerCase());
        clearIndex(CsarGitRepository.class.getSimpleName().toLowerCase());
        clearIndex(PaaSDeploymentLog.class.getSimpleName().toLowerCase());

        clearIndex(AuditESDAO.ALIEN_AUDIT_INDEX, AuditTrace.class.getSimpleName().toLowerCase());
        clearIndex(AuditESDAO.ALIEN_AUDIT_INDEX, AuditConfiguration.class.getSimpleName().toLowerCase());

        clearIndex(ElasticSearchDAO.SUGGESTION_INDEX, AbstractSuggestionEntry.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.SUGGESTION_INDEX, SuggestionEntry.class.getSimpleName().toLowerCase());
        clearIndex(ElasticSearchDAO.SUGGESTION_INDEX, SimpleSuggestionEntry.class.getSimpleName().toLowerCase());

        clearIndex(Repository.class.getSimpleName().toLowerCase());
        clearIndex(ServiceResource.class.getSimpleName().toLowerCase());

        clearIndex(MaintenanceModeState.class.getSimpleName().toLowerCase());
        clearIndex(Plugin.class.getSimpleName().toLowerCase());
        clearIndex(PluginConfiguration.class.getSimpleName().toLowerCase());


        // clean things in Context
        Context.getInstance().clearComponentsIds();
        Context.getInstance().takeTopologyId();
        Context.getInstance().takeApplication();
        Context.getRestClientInstance().clearCookies();
        Context.getInstance().takePreRegisteredOrchestratorProperties();
        Context.getInstance().clearEnvironmentInfos();
        ApplicationStepDefinitions.CURRENT_APPLICATIONS.clear();
        ApplicationStepDefinitions.CURRENT_APPLICATION = null;
    }

    @Then("^I should receive a RestResponse with no error$")
    public static void I_should_receive_a_RestResponse_with_no_error() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        if (restResponse.getError() != null) {
            log.error("Rest response was <" + Context.getInstance().getRestResponse() + ">");
            log.error("data are: " + restResponse.getData());
        }
        Assert.assertNull(restResponse.getError());
    }

    @Then("^I should receive a RestResponse with an error code (\\d+)$")
    public void I_should_receive_a_RestResponse_with_an_error_code(int expectedCode) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
        Assert.assertNotNull(restResponse.getError());
        Assert.assertEquals(expectedCode, restResponse.getError().getCode());
    }

    @Then("^I should receive a RestResponse with an error code (\\d+) and a message containing \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_an_error_code_and_a_message_containing(int expectedCode, String messageToCheck) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse.getError());
        Assert.assertEquals(expectedCode, restResponse.getError().getCode());
        Assert.assertTrue(restResponse.getError().getMessage().contains(messageToCheck));
    }

    @Then("^I should receive a RestResponse with an error code (\\d+) and a field error with field \"([^\"]*)\" and code \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_an_error_code_and_a_field_error_with_field_and_code(int expectedCode, String expectedFieldName,
            String expectedFieldErrorCode) throws Throwable {
        RestResponse<FieldErrorDTO[]> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FieldErrorDTO[].class);
        Assert.assertNotNull(restResponse.getError());
        Assert.assertEquals(expectedCode, restResponse.getError().getCode());
        boolean containsField = false;
        for (FieldErrorDTO fieldError : restResponse.getData()) {
            if (expectedFieldName.equals(fieldError.getField())) {
                containsField = true;
                Assert.assertEquals(expectedFieldErrorCode, fieldError.getCode());
            }
        }
        Assert.assertTrue(containsField);
    }

    @Then("^I should receive a RestResponse with no data$")
    public void I_should_receive_a_RestResponse_with_no_data() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNull(restResponse.getData());
    }

    @Then("^I should receive a RestResponse with a boolean data \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_a_boolean_data(String data) throws Throwable {
        RestResponse<Boolean> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), Boolean.class);
        Assert.assertNotNull(restResponse.getData());
        boolean expected = Boolean.valueOf(data);
        Assert.assertEquals(expected, (boolean) restResponse.getData());
    }

    @Then("^I should receive a RestResponse with a string data \"([^\"]*)\"$")
    public void I_should_receive_a_RestResponse_with_a_string_data(String expectedResponseStr) throws Throwable {
        RestResponse<String> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
        Assert.assertNotNull(restResponse.getData());
        Assert.assertEquals(expectedResponseStr, restResponse.getData());
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentContext(String spelExpression, String expected) {
        evaluateAndAssertExpression(Context.getInstance().getSpelEvaluationContext(), spelExpression, expected);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should contains \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentContext2(String spelExpression, String expectedPart) throws Throwable {
        evaluateAndAssertExpressionContains(Context.getInstance().getSpelEvaluationContext(), spelExpression, expectedPart);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return (true|false)$")
    public void evaluateSpelExpressionUsingCurrentTopologyContext(String spelExpression, Boolean expected) {
        evaluateAndAssertExpression(Context.getInstance().getSpelEvaluationContext(), spelExpression, expected);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return (\\d+)$")
    public void evaluateSpelExpressionUsingCurrentTopologyContext(String spelExpression, Integer expected) {
        evaluateAndAssertExpression(Context.getInstance().getSpelEvaluationContext(), spelExpression, expected);
    }

    @Deprecated
    @Then("^The SPEL int expression \"([^\"]*)\" should return (\\d+)$")
    public void The_SPEL_int_expression_should_return(String spelExpression, int expected) throws Throwable {
        evaluateAndAssertExpression(Context.getInstance().getSpelEvaluationContext(), spelExpression, expected);
    }

    @Deprecated
    @Then("^The SPEL boolean expression \"([^\"]*)\" should return (true|false)$")
    public void evaluateSpelBooleanExpressionUsingCurrentContext(String spelExpression, Boolean expected) {
        evaluateAndAssertExpression(Context.getInstance().getSpelEvaluationContext(), spelExpression, expected);
    }

    @When("^I register the rest response data as SPEL context of type \"([^\"]*)\"$")
    public void I_register_the_rest_response_data_as_SPEL_context_of_type(String type) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse(), Class.forName(type), Context.getJsonMapper());
        Context.getInstance().buildEvaluationContext(response.getData());
    }

    @When("^I register the rest response data as SPEL context$")
    public void I_register_the_rest_response_data_as_SPEL_context() throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse(), Context.getJsonMapper());
        Context.getInstance().buildEvaluationContext(response.getData());
    }

    @When("^I register the rest response data as SPEL context of type2 \"([^\"]*)\"$")
    /**
     * @deprecated use "I register the rest response data as SPEL context of type" instead
     */
    @Deprecated
    public void I_register_the_rest_response_data_as_SPEL_context2(String type) throws Throwable {
        I_register_the_rest_response_data_as_SPEL_context_of_type(type);
    }

    @Then("^Response should contains (\\d+) items$")
    public void Response_should_contains_items(int count) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        Assert.assertEquals(count, response.getData().getTotalResults());
    }

    @Then("^I should receive a RestResponse with a non empty string data$")
    public void i_Should_Receive_A_RestResponse_With_A_Non_Empty_String_Data() throws Throwable {
        RestResponse<String> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), String.class);
        Assert.assertTrue(StringUtils.isNotBlank(restResponse.getData()));
    }

    /**
     * Validate that the last request made didn't throw any error
     * 
     * @param successfully
     * @throws Throwable
     */
    public static void validateIfNeeded(Boolean successfully) throws Throwable {
        if (successfully) {
            I_should_receive_a_RestResponse_with_no_error();
        }
    }

    @And("^I should wait for (\\d+) seconds before continuing the test$")
    public void I_should_wait_for_seconds_before_continuing_the_test(int sleepTimeInSeconds) throws Throwable {
        I_wait_for_seconds_before_continuing_the_test(sleepTimeInSeconds);
    }

    @And("^I wait for (\\d+) seconds before continuing the test$")
    public void I_wait_for_seconds_before_continuing_the_test(int sleepTimeInSeconds) throws Throwable {
        log.info("Begin sleeping to wait before continuing the test");
        Thread.sleep(sleepTimeInSeconds * 1000L);
    }
}
