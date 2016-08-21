package alien4cloud.it.common;

import java.nio.file.Files;
import java.util.List;

import alien4cloud.model.repository.Repository;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import alien4cloud.audit.AuditESDAO;
import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.components.Csar;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.model.PluginConfiguration;
import org.alien4cloud.exception.rest.FieldErrorDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.Group;
import alien4cloud.security.model.User;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommonStepDefinitions {
    private final Client esClient = Context.getEsClientInstance();
    private List<String> indicesToClean;

    public CommonStepDefinitions() {
        indicesToClean = Lists.newArrayList();
        indicesToClean.add(ApplicationEnvironment.class.getSimpleName().toLowerCase());
        indicesToClean.add(ApplicationVersion.class.getSimpleName().toLowerCase());
        indicesToClean.add(DeploymentTopology.class.getSimpleName().toLowerCase());
        indicesToClean.add(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
        indicesToClean.add(Application.class.getSimpleName().toLowerCase());
        indicesToClean.add(Orchestrator.class.getSimpleName().toLowerCase());
        indicesToClean.add(Location.class.getSimpleName().toLowerCase());
        indicesToClean.add(Csar.class.getSimpleName().toLowerCase());
        indicesToClean.add(Topology.class.getSimpleName().toLowerCase());
        indicesToClean.add(TopologyTemplate.class.getSimpleName().toLowerCase());
        indicesToClean.add(Deployment.class.getSimpleName().toLowerCase());
        indicesToClean.add(Group.class.getSimpleName().toLowerCase());
        indicesToClean.add(User.class.getSimpleName().toLowerCase());
        indicesToClean.add(MetaPropConfiguration.class.getSimpleName().toLowerCase());
        indicesToClean.add(CsarGitRepository.class.getSimpleName().toLowerCase());
        indicesToClean.add(PaaSDeploymentLog.class.getSimpleName().toLowerCase());
        indicesToClean.add(AuditESDAO.ALIEN_AUDIT_INDEX);
        indicesToClean.add(ElasticSearchDAO.SUGGESTION_INDEX);
        indicesToClean.add(Repository.class.getSimpleName().toLowerCase());

        indicesToClean.add(Plugin.class.getSimpleName().toLowerCase());
        indicesToClean.add(PluginConfiguration.class.getSimpleName().toLowerCase());
    }

    @Before(value = "@reset", order = 1)
    public void beforeScenario() throws Throwable {
        // teardown the platform before removing all data
        // connect as admin
        AuthenticationStepDefinitions authenticationStepDefinitions = new AuthenticationStepDefinitions();
        authenticationStepDefinitions.I_am_authenticated_with_role("ADMIN");
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
        for (String index : indicesToClean) {
            esClient.prepareDeleteByQuery(new String[] { index }).setQuery(QueryBuilders.matchAllQuery()).execute().get();
        }

        // clean things in Context
        Context.getInstance().clearComponentsIds();
        Context.getInstance().takeTopologyId();
        Context.getInstance().takeApplication();
        Context.getRestClientInstance().clearCookies();
        Context.getInstance().takePreRegisteredOrchestratorProperties();
    }

    @Then("^I should receive a RestResponse with no error$")
    public void I_should_receive_a_RestResponse_with_no_error() throws Throwable {
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

    @Then("^The SPEL boolean expression \"([^\"]*)\" should return true$")
    public void evaluateSpelBooleanExpressionUsingCurrentContext(String spelExpression) {
        Boolean result = (Boolean) evaluateExpression(spelExpression);
        Assert.assertTrue(String.format("The SPEL expression [%s] should return true as a result", spelExpression), result.booleanValue());
    }

    private Object evaluateExpression(String spelExpression) {
        EvaluationContext context = Context.getInstance().getSpelEvaluationContext();
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(spelExpression);
        return exp.getValue(context);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentContext(String spelExpression, String expected) {
        String result = evaluateExpression(spelExpression).toString();
        Assert.assertNotNull(String.format("The SPEL expression [%s] result should not be null", spelExpression), result);
        Assert.assertEquals(String.format("The SPEL expression [%s] should return [%s]", spelExpression, expected), expected, result);
    }

    @Then("^The SPEL int expression \"([^\"]*)\" should return (\\d+)$")
    public void The_SPEL_int_expression_should_return(String spelExpression, int expected) throws Throwable {
        Integer actual = (Integer) evaluateExpression(spelExpression);
        Assert.assertEquals(String.format("The SPEL expression [%s] should return [%d]", spelExpression, expected), expected, actual.intValue());
    }

    @When("^I register the rest response data as SPEL context of type \"([^\"]*)\"$")
    public void I_register_the_rest_response_data_as_SPEL_context(String type) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse(), Class.forName(type));
        Context.getInstance().buildEvaluationContext(response.getData());
    }

    @When("^I register the rest response data as SPEL context of type2 \"([^\"]*)\"$")
    public void I_register_the_rest_response_data_as_SPEL_context2(String type) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse(), Class.forName(type), Context.getJsonMapper());
        Context.getInstance().buildEvaluationContext(response.getData());
    }

    @Then("^Response should contains (\\d+) items$")
    public void Response_should_contains_items(int count) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        Assert.assertEquals(count, response.getData().getTotalResults());
    }
}