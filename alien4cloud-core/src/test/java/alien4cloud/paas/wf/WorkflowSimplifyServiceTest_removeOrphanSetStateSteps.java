package alien4cloud.paas.wf;

import java.io.IOException;

import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import alien4cloud.paas.wf.model.WorkflowTest;
import alien4cloud.paas.wf.model.WorkflowTestUtils;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by xdegenne on 22/06/2018.
 */
@Slf4j
public class WorkflowSimplifyServiceTest_removeOrphanSetStateSteps extends WorkflowTestBase {

    @Test
    public void test() throws IOException {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resourcePatternResolver.getResources("alien4cloud/paas/workflow/remove_orphan_set_state_steps/*.yml");
        for (Resource resource : resources) {
            WorkflowTest test = parseWorkflow(resource);
            doTest(resource.getFilename(), test);
        }
    }

    private void doTest(String testName, WorkflowTest test) {
        log.info("Testing {}", testName);
        log.debug("Initial workflow : " + WorkflowUtils.debugWorkflow(test.getInitial()));
        workflowSimplifyService.removeOrphanSetStateSteps(defaultDeclarativeWorkflows, test.getInitial());
        log.debug("Actual workflow : " + WorkflowUtils.debugWorkflow(test.getInitial()));
        log.debug("Excpected workflow : " + WorkflowUtils.debugWorkflow(test.getExpected()));
        WorkflowTestUtils.assertSame(test.getExpected(), test.getInitial());
    }

}
