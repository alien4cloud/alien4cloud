package alien4cloud.it.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Lists;

import alien4cloud.it.Context;
import alien4cloud.paas.wf.AbstractActivity;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.topology.TopologyWorkflowAddActivityRequest;
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

    @When("^I remove the workflow step named \"(.*?)\"$")
    public void i_remove_the_workflow_step_named(String stepName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/steps/%s", topologyId, workflowName, stepName);
        String restResponse = Context.getRestClientInstance().delete(path);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @Given("^I rename the workflow step named \"(.*?)\" to \"(.*?)\"$")
    public void i_rename_the_workflow_step_named_to(String stepName, String newStepName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/steps/%s", topologyId, workflowName, stepName);
        List<NameValuePair> npvs = Lists.newArrayList();
        npvs.add(new BasicNameValuePair("newStepName", newStepName));
        String restResponse = Context.getRestClientInstance().postUrlEncoded(path, npvs);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I append a call operation \"(.*?)\" activity for node \"(.*?)\" after the step \"(.*?)\"$")
    public void i_add_a_call_operation_activity_for_node_after_the_step(String operationFqn, String nodeId, String relatedStepId) throws Throwable {
        addCallOperationActivity(operationFqn, nodeId, relatedStepId, false);
    }

    @When("^I insert a call operation \"(.*?)\" activity for node \"(.*?)\" before the step \"(.*?)\"$")
    public void i_add_a_call_operation_activity_for_node_before_the_step(String operationFqn, String nodeId, String relatedStepId) throws Throwable {
        addCallOperationActivity(operationFqn, nodeId, relatedStepId, true);
    }

    private void addCallOperationActivity(String operationFqn, String nodeId, String relatedStepId, boolean before) throws Throwable {
        OperationCallActivity activity = new OperationCallActivity();
        activity.setNodeId(nodeId);
        activity.setOperationFqn(operationFqn);
        addActivity(activity, relatedStepId, before);
    }

    private void addActivity(AbstractActivity activity, String relatedStepId, boolean before) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        TopologyWorkflowAddActivityRequest req = new TopologyWorkflowAddActivityRequest();
        req.setActivity(activity);
        req.setRelatedStepId(relatedStepId);
        req.setBefore(before);
        String path = String.format("/rest/topologies/%s/workflows/%s/activities", topologyId, workflowName);
        String restResponse = Context.getRestClientInstance().postJSon(path, JsonUtil.toString(req));
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I add a call operation \"(.*?)\" activity for node \"(.*?)\"$")
    public void i_add_a_call_operation_activity_for_node(String operationFqn, String nodeId) throws Throwable {
        i_add_a_call_operation_activity_for_node_after_the_step(operationFqn, nodeId, null);
    }

    @When("^I append a set state \"(.*?)\" activity for node \"(.*?)\" after the step \"(.*?)\"$")
    public void i_add_set_state_activity_for_node_after_the_step(String stateName, String nodeId, String relatedStepId) throws Throwable {
        addSetStateActivity(stateName, nodeId, relatedStepId, false);
    }

    @When("^I insert a set state \"(.*?)\" activity for node \"(.*?)\" before the step \"(.*?)\"$")
    public void i_add_set_state_activity_for_node_before_the_step(String stateName, String nodeId, String relatedStepId) throws Throwable {
        addSetStateActivity(stateName, nodeId, relatedStepId, true);
    }

    private void addSetStateActivity(String stateName, String nodeId, String relatedStepId, boolean before) throws Throwable {
        SetStateActivity activity = new SetStateActivity();
        activity.setNodeId(nodeId);
        activity.setStateName(stateName);
        addActivity(activity, relatedStepId, before);
    }

    @When("^I add a set state \"(.*?)\" activity for node \"(.*?)\"$")
    public void i_add_set_state_activity_for_node(String stateName, String nodeId) throws Throwable {
        i_add_set_state_activity_for_node_after_the_step(stateName, nodeId, null);
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

    @When("^I swap the workflow step \"(.*?)\" with \"(.*?)\"$")
    public void i_swap_the_workflow_step_with(String stepId, String targetId) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/steps/%s/swap", topologyId, workflowName, stepId);
        List<NameValuePair> npvs = Lists.newArrayList();
        npvs.add(new BasicNameValuePair("targetId", targetId));
        String restResponse = Context.getRestClientInstance().postUrlEncoded(path, npvs);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @Given("^I connect the workflow step \"(.*?)\" to: (.*)$")
    public void i_connect_the_workflow_step_to(String stepId, List<String> targetIds) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/steps/%s/connectTo", topologyId, workflowName, stepId);
        connect_the_workflow_steps(path, targetIds);
    }

    private void connect_the_workflow_steps(String path, List<String> targetIds) throws Throwable {
        String[] targets = new String[targetIds.size()];
        int i = 0;
        for (String targetId : targetIds) {
            // we just remove the surrounding quotes
            targetId = targetId.substring(1, targetId.length() - 1);
            targets[i++] = targetId;
        }
        String restResponse = Context.getRestClientInstance().postJSon(path, JsonUtil.toString(targets));
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I connect the workflow step \"(.*?)\" from: (.*)$")
    public void i_connect_the_workflow_step_from(String stepId, List<String> targetIds) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/steps/%s/connectFrom", topologyId, workflowName, stepId);
        connect_the_workflow_steps(path, targetIds);
    }

    @When("^I disconnect the workflow step from \"(.*?)\" to \"(.*?)\"$")
    public void i_disconnect_the_workflow_step_from(String from, String to) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/edges/%s/%s", topologyId, workflowName, from, to);
        String restResponse = Context.getRestClientInstance().delete(path);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I reset the workflow$")
    public void i_reset_the_workflow() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s/init", topologyId, workflowName);
        String restResponse = Context.getRestClientInstance().postJSon(path, "");
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I rename the workflow to \"(.*?)\"$")
    public void i_rename_the_workflow_to(String newName) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s", topologyId, workflowName);
        List<NameValuePair> npvs = Lists.newArrayList();
        npvs.add(new BasicNameValuePair("newName", newName));
        String restResponse = Context.getRestClientInstance().postUrlEncoded(path, npvs);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I remove the workflow$")
    public void i_remove_the_workflow() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String workflowName = Context.getInstance().getCurrentWorkflowName();
        String path = String.format("/rest/topologies/%s/workflows/%s", topologyId, workflowName);
        String restResponse = Context.getRestClientInstance().delete(path);
        Context.getInstance().registerRestResponse(restResponse);
    }

    @When("^I create a new custom workflow$")
    public void i_create_a_new_custom_workflow() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        String path = String.format("/rest/topologies/%s/workflows", topologyId);
        String restResponse = Context.getRestClientInstance().postJSon(path, "");
        Context.getInstance().registerRestResponse(restResponse);
    }

    @Then("^the workflow should exist in the topology and I start editing it$")
    public void the_workflow_should_exist_in_the_topology_and_I_start_editing_it() throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();
        RestResponse<Workflow> workflowResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), Workflow.class, Context.getJsonMapper());
        String workflowName = workflowResponse.getData().getName();
        String topologyRestResponse = Context.getRestClientInstance().get("/rest/topologies/" + topologyId);
        RestResponse<TopologyDTO> topologyResponse = JsonUtil.read(topologyRestResponse, TopologyDTO.class, Context.getJsonMapper());
        assertTrue(topologyResponse.getData().getTopology().getWorkflows().containsKey(workflowName));
        Context.getInstance().setCurrentWorkflowName(workflowName);
    }

}
