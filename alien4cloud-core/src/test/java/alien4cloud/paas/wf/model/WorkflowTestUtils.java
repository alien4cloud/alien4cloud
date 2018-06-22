package alien4cloud.paas.wf.model;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.junit.Assert;

/**
 * Created by xdegenne on 22/06/2018.
 */
public class WorkflowTestUtils {

    public static void assertSame(Workflow expected, Workflow actual) {
        Assert.assertEquals("Workflow don't have the same number of step", actual.getSteps().size(), expected.getSteps().size());
        expected.getSteps().forEach((stepName, expectedStep) -> {
            WorkflowStep actualStep = actual.getSteps().get(stepName);
            if (actualStep == null) {
                Assert.fail(String.format("The expected step %s is not found in actual wf", stepName));
            }
            Assert.assertEquals("OnSucess are not as expected for step " + stepName, expectedStep.getOnSuccess(), actualStep.getOnSuccess());
            Assert.assertEquals("PrecedingSteps are not as expected for step " + stepName, expectedStep.getPrecedingSteps(), actualStep.getPrecedingSteps());
        });
    }

}
