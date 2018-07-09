package alien4cloud.paas.wf;

import java.io.IOException;

import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import alien4cloud.paas.wf.model.WorkflowTest;
import alien4cloud.paas.wf.model.WorkflowTestUtils;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowSimplifyServiceTest_removeUselessEdges extends WorkflowTestBase {

    @Test
    public void test() throws IOException {
        PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resourcePatternResolver.getResources("alien4cloud/paas/workflow/remove_useless_edges/*.yml");
        for (Resource resource : resources) {
            WorkflowTest test = parseWorkflow(resource);
            doTest(resource.getFilename(), test);
        }
    }

    private void doTest(String testName, WorkflowTest test) {
        log.info("Testing {}", testName);
        log.debug("Initial workflow : " + WorkflowUtils.debugWorkflow(test.getInitial()));
        workflowSimplifyService.removeUselessEdges(test.getInitial());
        log.debug("Actual workflow : " + WorkflowUtils.debugWorkflow(test.getInitial()));
        log.debug("Excpected workflow : " + WorkflowUtils.debugWorkflow(test.getExpected()));
        WorkflowTestUtils.assertSame(test.getExpected(), test.getInitial());
    }

}
