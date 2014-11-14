package alien4cloud.it.common;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;

import alien4cloud.dao.ElasticSearchDAO;
import alien4cloud.it.Context;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudConfiguration;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.PluginConfiguration;
import alien4cloud.rest.exception.FieldErrorDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.User;
import alien4cloud.security.groups.Group;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.tosca.container.model.topology.TopologyTemplate;
import alien4cloud.tosca.model.Csar;
import alien4cloud.utils.FileUtil;
import cucumber.api.java.Before;
import cucumber.api.java.en.Then;

@Slf4j
public class CommonStepDefinitions {
    private final Client esClient = Context.getEsClientInstance();
    private List<String> indicesToClean;
    private final ITCsarRetriever csarRetriever = new ITCsarRetriever();

    public CommonStepDefinitions() {
        indicesToClean = Lists.newArrayList();
        indicesToClean.add(ApplicationEnvironment.class.getSimpleName().toLowerCase());
        indicesToClean.add(ApplicationVersion.class.getSimpleName().toLowerCase());
        indicesToClean.add(DeploymentSetup.class.getSimpleName().toLowerCase());
        indicesToClean.add(ElasticSearchDAO.TOSCA_ELEMENT_INDEX);
        indicesToClean.add(Application.class.getSimpleName().toLowerCase());
        indicesToClean.add(Csar.class.getSimpleName().toLowerCase());
        indicesToClean.add(Topology.class.getSimpleName().toLowerCase());
        indicesToClean.add(TopologyTemplate.class.getSimpleName().toLowerCase());
        indicesToClean.add(Plugin.class.getSimpleName().toLowerCase());
        indicesToClean.add(PluginConfiguration.class.getSimpleName().toLowerCase());
        indicesToClean.add(Cloud.class.getSimpleName().toLowerCase());
        indicesToClean.add(CloudConfiguration.class.getSimpleName().toLowerCase());
        indicesToClean.add(Deployment.class.getSimpleName().toLowerCase());
        indicesToClean.add(Group.class.getSimpleName().toLowerCase());
        indicesToClean.add(User.class.getSimpleName().toLowerCase());
        indicesToClean.add(CloudImage.class.getSimpleName().toLowerCase());
    }

    @Before
    public void beforeScenario() throws IOException, InterruptedException, ExecutionException {
        csarRetriever.retrieveGitArtifacts();

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
        if (Files.exists(Context.getInstance().getArtifactDirPath())) {
            log.debug("Removing artifact directory [" + Context.getInstance().getArtifactDirPath().toAbsolutePath() + "]");
            FileUtil.delete(Context.getInstance().getArtifactDirPath());
        }

        Files.createDirectories(Context.getInstance().getTmpDirectory());
        Files.createDirectories(Context.getInstance().getRepositoryDirPath());
        Files.createDirectories(Context.getInstance().getUploadTempDirPath());
        Files.createDirectories(Context.getInstance().getPluginDirPath());
        Files.createDirectories(Context.getInstance().getArtifactDirPath());

        // Clean elastic search cluster
        for (String indice : indicesToClean) {
            esClient.prepareDeleteByQuery(new String[] { indice }).setQuery(QueryBuilders.matchAllQuery()).execute().get();
        }

        // clean things in Context
        Context.getInstance().clearComponentsIds();
        Context.getInstance().takeTopologyId();
        Context.getInstance().takeApplication();
        Context.getRestClientInstance().clearCookies();
    }

    @Then("^I should receive a RestResponse with no error$")
    public void I_should_receive_a_RestResponse_with_no_error() throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        if (restResponse.getError() != null) {
            log.debug("Rest response was <" + Context.getInstance().getRestResponse() + ">");
        }
        Assert.assertNull(restResponse.getError());
    }

    @Then("^I should receive a RestResponse with an error code (\\d+)$")
    public void I_should_receive_a_RestResponse_with_an_error_code(int expectedCode) throws Throwable {
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
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
}