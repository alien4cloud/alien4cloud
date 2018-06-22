package alien4cloud.paas.wf;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.utils.YamlParserUtil;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by xdegenne on 22/06/2018.
 */
public class WorkflowSimplifyServiceTest_removeOrphanSetStateSteps {

    WorkflowSimplifyService workflowSimplifyService = new WorkflowSimplifyService();
    DefaultDeclarativeWorkflows defaultDeclarativeWorkflows;

    @Before
    public void setup() throws IOException {
        defaultDeclarativeWorkflows = YamlParserUtil.parse(
                DefaultDeclarativeWorkflows.class.getClassLoader().getResourceAsStream("declarative-workflows-2.0.0-jobs.yml"), DefaultDeclarativeWorkflows.class);
    }

    @Test
    public void testSimpliest() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowUtils.linkSteps(node1_initial, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("We should only have 1 step here", 1, workflow.getSteps().size());
    }

    @Test
    public void testSimpliest2nodes() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowStep node2_initial = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node2_started = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.STARTED);
        WorkflowStep node2_starting = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.STARTING);
        WorkflowUtils.linkSteps(node1_initial, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_started);
        WorkflowUtils.linkSteps(node2_initial, node2_starting);
        WorkflowUtils.linkSteps(node2_starting, node2_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("We should only have 2 step here", 2, workflow.getSteps().size());
    }

    @Test
    public void testSimpliest2nodesLinked() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowStep node2_initial = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node2_started = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.STARTED);
        WorkflowStep node2_starting = WorkflowUtils.addStateStep(workflow, "node2", ToscaNodeLifecycleConstants.STARTING);
        WorkflowUtils.linkSteps(node1_initial, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_started);
        WorkflowUtils.linkSteps(node1_started, node2_initial);
        WorkflowUtils.linkSteps(node2_initial, node2_starting);
        WorkflowUtils.linkSteps(node2_starting, node2_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("We should only have 2 step here", 2, workflow.getSteps().size());
        assertFollowedOnlyBy(node1_initial, node2_initial);
    }

    @Test
    public void test1NodeStart() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowStep node1_start = WorkflowUtils.addOperationStep(workflow, "node1", ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.START);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(node1_initial, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_start);
        WorkflowUtils.linkSteps(node1_start, node1_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("We should only have 4 step here", 4, workflow.getSteps().size());
        assertFollowedOnlyBy(node1_initial, node1_starting);
        assertFollowedOnlyBy(node1_starting, node1_start);
        assertFollowedOnlyBy(node1_start, node1_started);
    }

    @Test
    public void test1NodeNoOperation() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_creating = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CREATING);
        WorkflowStep node1_created = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CREATED);
        WorkflowStep node1_configuring = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CONFIGURING);
        WorkflowStep node1_configured = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CONFIGURED);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(node1_initial, node1_creating);
        WorkflowUtils.linkSteps(node1_creating, node1_created);
        WorkflowUtils.linkSteps(node1_created, node1_configuring);
        WorkflowUtils.linkSteps(node1_configuring, node1_configured);
        WorkflowUtils.linkSteps(node1_configured, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("We should only have 1 step here", 1, workflow.getSteps().size());
    }

    @Test
    public void test1NodeAllOperations() {
        Workflow workflow = new Workflow();
        workflow.setName("install");
        WorkflowStep node1_initial = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowStep node1_creating = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CREATING);
        WorkflowStep node1_create = WorkflowUtils.addOperationStep(workflow, "node1", ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.CREATE);
        WorkflowStep node1_created = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CREATED);
        WorkflowStep node1_configuring = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CONFIGURING);
        WorkflowStep node1_configure = WorkflowUtils.addOperationStep(workflow, "node1", ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.CONFIGURE);
        WorkflowStep node1_configured = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.CONFIGURED);
        WorkflowStep node1_starting = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTING);
        WorkflowStep node1_start = WorkflowUtils.addOperationStep(workflow, "node1", ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.START);
        WorkflowStep node1_started = WorkflowUtils.addStateStep(workflow, "node1", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(node1_initial, node1_creating);
        WorkflowUtils.linkSteps(node1_creating, node1_create);
        WorkflowUtils.linkSteps(node1_create, node1_created);
        WorkflowUtils.linkSteps(node1_created, node1_configuring);
        WorkflowUtils.linkSteps(node1_configuring, node1_configure);
        WorkflowUtils.linkSteps(node1_configure, node1_configured);
        WorkflowUtils.linkSteps(node1_configured, node1_starting);
        WorkflowUtils.linkSteps(node1_starting, node1_start);
        WorkflowUtils.linkSteps(node1_start, node1_started);

        workflowSimplifyService.removeOrphanSetStateSteps(workflow, defaultDeclarativeWorkflows);
        WorkflowUtils.debugWorkflow(workflow);
        Assert.assertEquals("All steps should remain", 10, workflow.getSteps().size());
        assertFollowedOnlyBy(node1_initial, node1_creating);
        assertFollowedOnlyBy(node1_creating, node1_create);
        assertFollowedOnlyBy(node1_create, node1_created);
        assertFollowedOnlyBy(node1_created, node1_configuring);
        assertFollowedOnlyBy(node1_configuring, node1_configure);
        assertFollowedOnlyBy(node1_configure, node1_configured);
        assertFollowedOnlyBy(node1_configured, node1_starting);
        assertFollowedOnlyBy(node1_starting, node1_start);
        assertFollowedOnlyBy(node1_start, node1_started);
    }

    private void assertFollowedOnlyBy(WorkflowStep step1, WorkflowStep step2) {
        Assert.assertEquals(String.format("%s should have 1 successor", step1.getName()), 1, step1.getOnSuccess().size());
        Assert.assertTrue(String.format("%s should be succeeded by %s", step1.getName(), step2.getName()), step1.getOnSuccess().contains(step2.getName()));
        Assert.assertTrue(String.format("%s should be preceeded by %s", step2.getName(), step1.getName()), step2.getPrecedingSteps().contains(step1.getName()));
    }

}
