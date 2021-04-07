package alien4cloud.paas.wf;

import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.junit.Ignore;
import org.junit.Test;

import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.CycleDetection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CycleDetectionTest extends AbstractValidationTest<CycleDetection> {

    private CycleDetection cycleDetectionRule = new CycleDetection();

    @Override
    protected CycleDetection getRule() {
        return cycleDetectionRule;
    }

    /**
     * <pre>
     * 
     *  a -- b
     * 
     * </pre>
     */
    @Test
    public void testNoCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a, b);
        processValidation(false, 0);
    }

    /**
     * <pre>
     * 
     *        /--|
     *  a -- b   |
     *        \--|
     * 
     * </pre>
     */
    @Test()
    public void testAutoCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, b);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *    --
     *   /  \
     *  a    b
     *   \  /
     *    --
     * </pre>
     */
    @Test
    public void test2StepsCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, a);
        processValidation(true, 1);
    }

    /**
     * <pre>
     * 
     * o --- c --- d
     * 
     *    --
     *   /  \
     *  a    b
     *   \  /
     *    --
     * </pre>
     */
    @Test
    public void testOrphanCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowStep d = wf.addStep(new SimpleStep("d"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, a);
        WorkflowUtils.linkSteps(c, d);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *        --
     *       /  \
     * a -- b    c
     *       \  /
     *        --
     * </pre>
     */
    @Test
    public void testIndirectCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, c);
        WorkflowUtils.linkSteps(c, b);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *      -- b
     *     /
     * a --
     *     \ 
     *      -- c -- d -- e
     * </pre>
     */
    @Test
    public void testForkJoinNoCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowStep d = wf.addStep(new SimpleStep("d"));
        WorkflowStep e = wf.addStep(new SimpleStep("e"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(a, c);
        WorkflowUtils.linkSteps(c, d);
        WorkflowUtils.linkSteps(d, e);
        processValidation(false, 0);
    }

    /**
     * <pre>
     *      -- b
     *     /
     * a --      e        
     *     \    /  \
     *      -- c -- d --
     * </pre>
     */
    @Test()
    public void testForkJoinCycle() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowStep d = wf.addStep(new SimpleStep("d"));
        WorkflowStep e = wf.addStep(new SimpleStep("e"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(a, c);
        WorkflowUtils.linkSteps(c, d);
        WorkflowUtils.linkSteps(d, e);
        WorkflowUtils.linkSteps(e, c);
        processValidation(true, 1);
    }

    /**
     * <pre>
     *           --- c
     *          /     \
     *      -- b ----- d
     *     /
     * a --      f        
     *     \    /  \
     *      -- e -- g
     * 
     * </pre>
     */
    @Test()
    public void testTwoParallelCycles() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowStep d = wf.addStep(new SimpleStep("d"));
        WorkflowStep e = wf.addStep(new SimpleStep("e"));
        WorkflowStep f = wf.addStep(new SimpleStep("f"));
        WorkflowStep g = wf.addStep(new SimpleStep("g"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, c);
        WorkflowUtils.linkSteps(c, d);
        WorkflowUtils.linkSteps(d, b);
        WorkflowUtils.linkSteps(a, e);
        WorkflowUtils.linkSteps(a, e);
        WorkflowUtils.linkSteps(e, f);
        WorkflowUtils.linkSteps(f, g);
        WorkflowUtils.linkSteps(g, e);
        // only 1 error since we fail fast while cycle detection
        processValidation(true, 1);
    }

    /**
     * <pre>
     * 
     *        c         f
     *       /  \      /  \
     * a -- b -- d -- e -- g
     * 
     * </pre>
     */
    @Test()
    public void testTwoSequencedCycles() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowStep c = wf.addStep(new SimpleStep("c"));
        WorkflowStep d = wf.addStep(new SimpleStep("d"));
        WorkflowStep e = wf.addStep(new SimpleStep("e"));
        WorkflowStep f = wf.addStep(new SimpleStep("f"));
        WorkflowStep g = wf.addStep(new SimpleStep("g"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, d);
        WorkflowUtils.linkSteps(d, c);
        WorkflowUtils.linkSteps(c, b);
        WorkflowUtils.linkSteps(d, e);
        WorkflowUtils.linkSteps(e, g);
        WorkflowUtils.linkSteps(g, f);
        WorkflowUtils.linkSteps(f, e);
        // only 1 error since we fail fast while cycle detection
        processValidation(true, 1);
    }

    @Test
    public void testOneCycleWithOnFailure() {
        WorkflowStep a = wf.addStep(new SimpleStep("a"));
        WorkflowStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a,b);
        WorkflowUtils.linkStepsWithOnFailure(b,a);
        processValidation(true, 1);
    }
}
