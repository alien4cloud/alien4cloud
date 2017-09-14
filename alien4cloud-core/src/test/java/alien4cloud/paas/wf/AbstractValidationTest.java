package alien4cloud.paas.wf;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.junit.Before;

import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.paas.wf.validation.AbstractWorkflowError;
import alien4cloud.paas.wf.validation.Rule;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractValidationTest<R extends Rule> {

    protected abstract R getRule();

    protected Workflow wf;

    @Before
    public void prepare() {
        wf = new Workflow();
        wf.setName(INSTALL);
    }

    protected void processValidation(boolean workflowShouldHaveErrors, int errorCount) {
        log.info(WorkflowUtils.debugWorkflow(wf));
        if (workflowShouldHaveErrors && errorCount == 0 || !workflowShouldHaveErrors && errorCount > 0) {
            fail("Bad assertion");
        }
        List<AbstractWorkflowError> errors = getRule().validate(null, wf);
        if (errors != null) {
            wf.addErrors(errors);
        }
        assertEquals(workflowShouldHaveErrors, wf.hasErrors());
        if (errorCount > 0) {
            assertNotNull(wf.getErrors());
            assertEquals(errorCount, wf.getErrors().size());
            for (AbstractWorkflowError e : wf.getErrors()) {
                log.info("Workflow validation error is : " + e);
            }
        }
    }

}
