package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.StateSequenceValidation;

@Slf4j
public class StateSequenceValidationTest extends AbstractValidationTest<StateSequenceValidation> {

    private StateSequenceValidation rule = new StateSequenceValidation();

    @Override
    protected StateSequenceValidation getRule() {
        return rule;
    }

    /**
     * a_ini -- a_cre
     */
    @Test
    public void testSimple() {
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        WorkflowUtils.linkSteps(a_ini, a_cre);
        processValidation(false, 0);
    }

    /**
     * a_cre -- a_ini
     */
    @Test
    public void testSimpleBadOrderFail() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowUtils.linkSteps(a_cre, a_ini);
        processValidation(true, 1);
    }

    /**
     * a_cre -- b_ini
     */
    @Test
    public void testSimple2Nodes() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        AbstractStep b_ini = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowUtils.linkSteps(a_cre, b_ini);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *     -- a_cre --
     *    /           \
     * ---              --
     *    \           /
     *     -- a_ini --
     * </pre>
     */
    @Test
    public void testSimpleParallelFail() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        processValidation(true, 1);
    }

    /**
     * a_ini -- step2 -- a_cre
     */
    @Test
    public void testSimpleMixed() {
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        SimpleStep step2 = wf.addStep(new SimpleStep("step2"));
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        WorkflowUtils.linkSteps(a_ini, step2);
        WorkflowUtils.linkSteps(step2, a_cre);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *     -- a_ini --      -- step3
     *    /           \    /
     * step1           a_cre
     *    \           /    \
     *     -- step2 --      -- a_con
     * </pre>
     */
    @Test
    public void testComplexe() {
        SimpleStep step1 = wf.addStep(new SimpleStep("step1"));
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        SimpleStep step2 = wf.addStep(new SimpleStep("step2"));
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        SimpleStep step3 = wf.addStep(new SimpleStep("step3"));
        AbstractStep a_con = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CONFIGURING);
        WorkflowUtils.linkSteps(step1, a_ini);
        WorkflowUtils.linkSteps(step1, step2);
        WorkflowUtils.linkSteps(a_ini, a_cre);
        WorkflowUtils.linkSteps(step2, a_cre);
        WorkflowUtils.linkSteps(a_cre, step3);
        WorkflowUtils.linkSteps(a_cre, a_con);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *     -- a_ini --      -- step3
     *    /           \    /
     * step1           a_con
     *    \           /    \
     *     -- step2 --      -- a_cre
     * </pre>
     */
    @Test
    public void testComplexeFailure() {
        SimpleStep step1 = wf.addStep(new SimpleStep("step1"));
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        SimpleStep step2 = wf.addStep(new SimpleStep("step2"));
        AbstractStep a_con = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CONFIGURING);
        SimpleStep step3 = wf.addStep(new SimpleStep("step3"));
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATING);
        WorkflowUtils.linkSteps(step1, a_ini);
        WorkflowUtils.linkSteps(step1, step2);
        WorkflowUtils.linkSteps(a_ini, a_con);
        WorkflowUtils.linkSteps(step2, a_con);
        WorkflowUtils.linkSteps(a_con, step3);
        WorkflowUtils.linkSteps(a_con, a_cre);
        processValidation(true, 1);
    }

    /**
     * <pre>
     * -- a_cre --- a_con --- a_sta
     *           / 
     *          /    
     *         /
     * -- b_cre --- b_con --- b_sta
     * </pre>
     */
    @Test
    public void testComplexe2Nodes() {
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep a_con = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep a_sta = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.STARTED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_sta = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(a_cre, a_con);
        WorkflowUtils.linkSteps(a_con, a_sta);
        WorkflowUtils.linkSteps(b_cre, b_con);
        WorkflowUtils.linkSteps(b_con, b_sta);
        WorkflowUtils.linkSteps(b_cre, a_con);
        processValidation(false, 0);
    }

    /**
     * <pre>
     * -- a_cre --- b_con --
     *         
     * -- b_cre --- b_sta --
     * </pre>
     */
    @Test
    public void testParallel() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_sta = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(a_cre, b_con);
        WorkflowUtils.linkSteps(b_cre, b_sta);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *    / -- a_cre --- b_con
     *   /            /    \
     *  o            /      \        o
     *   \          /        \      /
     *    \ -- b_cre ------- b_sta /
     * </pre>
     */
    @Test()
    public void test3() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_sta = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(a_cre, b_con);
        WorkflowUtils.linkSteps(b_cre, b_sta);
        WorkflowUtils.linkSteps(b_cre, b_con);
        WorkflowUtils.linkSteps(b_con, b_sta);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *    / -- a_cre --- b_con --
     *   /            /           \
     *  o            /              o
     *   \          /               /
     *    \ -- b_cre ------- b_sta /
     * </pre>
     */
    @Test
    public void test33() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_sta = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(a_cre, b_con);
        WorkflowUtils.linkSteps(b_cre, b_sta);
        WorkflowUtils.linkSteps(b_cre, b_con);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *    / -- a_cre --- a_ini ---
     *   /            /            \
     *  o            /              o
     *   \          /               /
     *    \ -- b_cre ---- b_ini ---/
     * </pre>
     */
    @Test
    public void test2errors() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep a_ini = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.INITIAL);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_init = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.INITIAL);
        WorkflowUtils.linkSteps(a_cre, a_ini);
        WorkflowUtils.linkSteps(b_cre, b_init);
        WorkflowUtils.linkSteps(b_cre, a_ini);
        processValidation(true, 2);
    }

    /**
     * <pre>
     *    / -- a_cre --- a_con --
     *   /            /           \
     *  o            /              o
     *   \          /               /
     *    \ -- b_cre ------- b_con /
     * </pre>
     */
    @Test()
    public void test333() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep a_con = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        WorkflowUtils.linkSteps(a_cre, a_con);
        WorkflowUtils.linkSteps(b_cre, b_con);
        WorkflowUtils.linkSteps(b_cre, a_con);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *    / -- a_cre ---  a_con --
     *   /            /           \
     *  o            /             \
     *   \          /               \
     *    \ -- b_cre --- b_con -- b_sta --- o
     * </pre>
     */
    @Test()
    public void test3333() {
        AbstractStep a_cre = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep a_con = buildStateStep(wf, "nodeA", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_cre = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CREATED);
        AbstractStep b_con = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.CONFIGURED);
        AbstractStep b_sta = buildStateStep(wf, "nodeB", ToscaNodeLifecycleConstants.STARTED);
        WorkflowUtils.linkSteps(a_cre, a_con);
        WorkflowUtils.linkSteps(b_cre, b_con);
        WorkflowUtils.linkSteps(b_cre, a_con);
        WorkflowUtils.linkSteps(b_con, b_sta);
        WorkflowUtils.linkSteps(a_con, b_sta);
        processValidation(false, 0);
    }

    private NodeActivityStep buildStateStep(Workflow wf, String nodeId, String stateName) {
        NodeActivityStep step = new NodeActivityStep();
        step.setNodeId(nodeId);
        SetStateActivity activity = new SetStateActivity();
        activity.setNodeId(nodeId);
        activity.setStateName(stateName);
        step.setActivity(activity);
        step.setName(WorkflowUtils.buildStepName(wf, step, 0));
        return wf.addStep(step);
    }

}
