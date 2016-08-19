package alien4cloud.it.topology;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import alien4cloud.it.Context;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class WorkflowStepDefinitions {

    @Given("^I edit the workflow named \"(.*?)\"$")
    public void i_edit_the_workflow_named(String workflowName) throws Throwable {
        Context.getInstance().setCurrentWorkflowName(workflowName);
    }

    @When("^The workflow step \"(.*?)\" is followed by: (.*)$")
    public void the_workflow_step_is_followed_by(String stepId, List<String> followers) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        Workflow workflow = topologyResponse.getData().getTopology().getWorkflows().get(workflowName);
        AbstractStep step = workflow.getSteps().get(stepId);
        Set<String> actualFollowers = step.getFollowingSteps();
        assertNotNull(actualFollowers);
        assertEquals(followers.size(), actualFollowers.size());
        for (String expectedFollower : followers) {
            // we just remove the surrounding quotes
            String follower = expectedFollower.substring(1, expectedFollower.length() - 1);
            assertTrue(actualFollowers.contains(follower));
        }
    }

    @When("^The workflow step \"(.*?)\" is preceded by: (.*)$")
    public void the_workflow_step_is_preceded_by(String stepId, List<String> predecesors) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        Workflow workflow = topologyResponse.getData().getTopology().getWorkflows().get(workflowName);
        AbstractStep step = workflow.getSteps().get(stepId);
        Set<String> actualPredecessors = step.getPrecedingSteps();
        assertNotNull(actualPredecessors);
        assertEquals(predecesors.size(), actualPredecessors.size());
        for (String expectedPredecessor : predecesors) {
            // we just remove the surrounding quotes
            String predecessor = expectedPredecessor.substring(1, expectedPredecessor.length() - 1);
            assertTrue(actualPredecessors.contains(predecessor));
        }
    }

    @Then("^The workflow step \"(.*?)\" has no followers$")
    public void the_workflow_step_has_no_followers(String stepId) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        Workflow workflow = topologyResponse.getData().getTopology().getWorkflows().get(workflowName);
        AbstractStep step = workflow.getSteps().get(stepId);
        assertTrue(step.getFollowingSteps() == null || step.getFollowingSteps().isEmpty());
    }

    @Then("^The workflow step \"(.*?)\" has no predecessors$")
    public void the_workflow_step_has_no_predecessors(String stepId) throws Throwable {
        String topologyResponseText = Context.getInstance().getRestResponse();
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyResponseText, TopologyDTO.class, Context.getJsonMapper());
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        Workflow workflow = topologyResponse.getData().getTopology().getWorkflows().get(workflowName);
        AbstractStep step = workflow.getSteps().get(stepId);
        assertTrue(step.getPrecedingSteps() == null || step.getPrecedingSteps().isEmpty());
    }

    @Then("^the workflow should exist in the topology and I start editing it$")
    public void the_workflow_should_exist_in_the_topology_and_I_start_editing_it() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        RestResponse<Workflow> workflowResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), Workflow.class, Context.getJsonMapper());
        String workflowName = workflowResponse.getData().getName();
        String topologyRestResponse = Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId);
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyRestResponse, TopologyDTO.class, Context.getJsonMapper());
        assertTrue(topologyResponse.getData().getTopology().getWorkflows().containsKey(workflowName));
        Context.getInstance().setCurrentWorkflowName(workflowName);
    }
}
