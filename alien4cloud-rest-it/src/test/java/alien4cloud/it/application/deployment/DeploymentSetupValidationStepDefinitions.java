package alien4cloud.it.application.deployment;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;

import com.google.common.collect.Maps;

import alien4cloud.it.Context;
import alien4cloud.it.utils.RegisteredStringUtils;
import alien4cloud.rest.deployment.DeploymentTopologyDTO;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.task.InputArtifactTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import cucumber.api.DataTable;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class DeploymentSetupValidationStepDefinitions {

    private DeploymentTopologyStepDefinitions deploymentTopologyStepDefinitions = new DeploymentTopologyStepDefinitions();

    private DeploymentTopologyDTO getDTOAndassertNotNull() throws IOException {
        String response = Context.getInstance().getRestResponse();
        DeploymentTopologyDTO dto = JsonUtil.read(response, DeploymentTopologyDTO.class, Context.getJsonMapper()).getData();
        assertNotNull(dto);
        return dto;
    }

    private Map<String, Object> parseAndReplaceProperties(Map<String, Object> inputProperties) {
        Map<String, Object> result = Maps.newHashMap();
        for (Entry<String, Object> entry : inputProperties.entrySet()) {
            result.put(entry.getKey(), RegisteredStringUtils.parseAndReplace(entry.getValue()));
        }
        return result;
    }

    @When("^I check for the valid status of the deployment topology$")
    public void iCheckForTheValidStatusOfTheDeploymentTopology() throws Throwable {
        deploymentTopologyStepDefinitions.I_get_the_deployment_toology_for_the_current_application();
    }

    @Then("^the deployment topology should not be valid$")
    public void theDeploymentTopologyShouldNotBeValid() throws Throwable {
        Assert.assertFalse("the deployment topology is valid", JsonUtil
                .read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData().getValidation().isValid());
    }

    @Then("^the deployment topology should be valid$")
    public void theDeploymentTopologyShouldBeValid() throws Throwable {
        Assert.assertTrue("the deployment topology is not valid", JsonUtil
                .read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData().getValidation().isValid());
    }

    @And("^the missing inputs artifacts should be$")
    public void theMissingInputsArtifactsShouldBe(DataTable expectedInputArtifactsTable) throws Throwable {
        TopologyValidationResult topologyValidationResult = JsonUtil
                .read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData().getValidation();
        for (List<String> expectedRow : expectedInputArtifactsTable.raw()) {
            boolean missingFound = topologyValidationResult.getTaskList().stream()
                    .anyMatch(task -> task instanceof InputArtifactTask && ((InputArtifactTask) task).getInputArtifactName().equals(expectedRow.get(0)));
            Assert.assertTrue(expectedRow.get(0) + " does not appear in the task list for the deployment topology", missingFound);
        }
    }

    @And("^the missing orchestrator properties should be$")
    public void theMissingOrchestratorPropertiesShouldBe(List<String> expectedInvalidOrchestratorProperties) throws Throwable {
        TopologyValidationResult topologyValidationResult = JsonUtil
                .read(Context.getInstance().getRestResponse(), DeploymentTopologyDTO.class, Context.getJsonMapper()).getData().getValidation();

        for (String expectedInvalidProp : expectedInvalidOrchestratorProperties) {
            boolean missingFound = topologyValidationResult.getTaskList().stream().anyMatch(task -> task.getCode().equals(TaskCode.ORCHESTRATOR_PROPERTY)
                    && ((PropertiesTask) task).getProperties().get(TaskLevel.REQUIRED).contains(expectedInvalidProp));
            Assert.assertTrue(expectedInvalidProp + " does not appear in invalid orchestrators properties task list for the deployment topology", missingFound);
        }
    }

}