package alien4cloud.paas.wf;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;

import java.util.List;

import org.junit.Test;

import alien4cloud.paas.wf.util.WorkflowGraphUtils;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

// TODO: asserts
@Slf4j
public class GraphPathsTest {

    /**
     * a -- b
     */
    @Test
    public void test1() {
        Workflow wf = new Workflow();
        wf.setStandard(true);
        wf.setName(INSTALL);
        AbstractStep a = wf.addStep(new SimpleStep("a"));
        AbstractStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a, b);
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(wf);
        log.info(paths.toString());
    }

    /**
     * -- a --
     * -- b --
     */
    @Test
    public void test2() {
        Workflow wf = new Workflow();
        wf.setStandard(true);
        wf.setName(INSTALL);
        AbstractStep a = wf.addStep(new SimpleStep("a"));
        AbstractStep b = wf.addStep(new SimpleStep("b"));
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(wf);
        log.info(paths.toString());
    }

    /**
     * <pre>
     *      c
     *     /
     * -- a 
     *     \ 
     *      b
     * </pre>
     */
    @Test
    public void test23() {
        Workflow wf = new Workflow();
        wf.setStandard(true);
        wf.setName(INSTALL);
        AbstractStep a = wf.addStep(new SimpleStep("a"));
        AbstractStep b = wf.addStep(new SimpleStep("b"));
        AbstractStep c = wf.addStep(new SimpleStep("c"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(a, c);
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(wf);
        log.info(paths.toString());
    }

    /**
     * <pre>
     *     -- b --      -- e
     *    /        \   /
     * a             d
     *    \        /   \
     *     -- c --      -- f
     * </pre>
     */
    @Test
    public void testComplexe() {
        Workflow wf = new Workflow();
        wf.setStandard(true);
        wf.setName(INSTALL);
        AbstractStep a = wf.addStep(new SimpleStep("a"));
        AbstractStep b = wf.addStep(new SimpleStep("b"));
        AbstractStep c = wf.addStep(new SimpleStep("c"));
        AbstractStep d = wf.addStep(new SimpleStep("d"));
        AbstractStep e = wf.addStep(new SimpleStep("e"));
        AbstractStep f = wf.addStep(new SimpleStep("f"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(a, c);
        WorkflowUtils.linkSteps(b, d);
        WorkflowUtils.linkSteps(c, d);
        WorkflowUtils.linkSteps(d, e);
        WorkflowUtils.linkSteps(d, f);
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(wf);
        log.info(paths.toString());
    }

    /**
     * <pre>
     *     -- a --
     *    /       \ 
     *    \       /
     *     -- b --
     * </pre>
     */
    @Test
    public void testOrphan() {
        Workflow wf = new Workflow();
        wf.setStandard(true);
        wf.setName(INSTALL);
        AbstractStep a = wf.addStep(new SimpleStep("a"));
        AbstractStep b = wf.addStep(new SimpleStep("b"));
        WorkflowUtils.linkSteps(a, b);
        WorkflowUtils.linkSteps(b, a);
        List<Path> paths = WorkflowGraphUtils.getWorkflowGraphPaths(wf);
        log.info(paths.toString());
    }
}
