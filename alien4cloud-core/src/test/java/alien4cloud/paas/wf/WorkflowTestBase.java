package alien4cloud.paas.wf;

import java.io.IOException;
import java.util.Map;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.declarative.DefaultDeclarativeWorkflows;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.springframework.core.io.Resource;

import com.google.common.collect.Maps;

import alien4cloud.paas.wf.model.WorkflowDescription;
import alien4cloud.paas.wf.model.WorkflowTest;
import alien4cloud.paas.wf.model.WorkflowTestDescription;
import alien4cloud.paas.wf.util.WorkflowUtils;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.YamlParserUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class WorkflowTestBase {

	protected WorkflowSimplifyService workflowSimplifyService = new WorkflowSimplifyService();
	protected DefaultDeclarativeWorkflows defaultDeclarativeWorkflows;

	@Before
	public void setup() throws IOException {
		defaultDeclarativeWorkflows = YamlParserUtil.parse(
				DefaultDeclarativeWorkflows.class.getClassLoader().getResourceAsStream("declarative-workflows-2.0.0-jobs.yml"), DefaultDeclarativeWorkflows.class);
	}

	protected WorkflowTest parseWorkflow(Resource testFileResource) throws IOException {
		WorkflowTestDescription wt = YamlParserUtil.parse(testFileResource.getInputStream(), WorkflowTestDescription.class);
		String workflowName = wt.getName();
		Workflow initial = buildWorkflow(workflowName, wt.getInitial());
		Workflow expected = buildWorkflow(workflowName, wt.getExpected());
		return new WorkflowTest(initial, expected);
	}

	protected Workflow buildWorkflow(String workflowName, WorkflowDescription wd) {
		Workflow workflow = new Workflow();
		workflow.setName(workflowName);
		Map<String, WorkflowStep> stepsPerId = Maps.newHashMap();
		// a first iteration to build steps
		AlienUtils.safe(wd.getSteps()).forEach((id, step) -> {
			WorkflowStep wfStep = null;
			if (StringUtils.isNoneEmpty(step.getRelation())) {
				wfStep = WorkflowUtils.addRelationshipOperationStep(workflow, step.getNode(), step.getRelation(), step.getInterf(), step.getOperation(), "");
			} else if (StringUtils.isNoneEmpty(step.getInterf())) {
				wfStep = WorkflowUtils.addOperationStep(workflow, step.getNode(), step.getInterf(), step.getOperation());
			} else if (StringUtils.isNoneEmpty(step.getState())) {
				wfStep = WorkflowUtils.addStateStep(workflow, step.getNode(), step.getState());
			} else {
				log.warn("Unrecognized step {}, will be ignored !", id);
				Assert.fail("Unrecognized step !");
			}
			if (wfStep != null) {
				stepsPerId.put(id, wfStep);
			}
		});
		// a second iteration to build links between steps
		AlienUtils.safe(wd.getSteps()).forEach((id, step) -> {
			WorkflowStep from = stepsPerId.get(id);
			AlienUtils.safe(step.getTo()).forEach(to -> {
				WorkflowStep toStep = stepsPerId.get(to);
				if (toStep == null) {
					log.warn("Link between {} -> {} not valid !", id, to);
					Assert.fail("Invalid step link !");
				}
				WorkflowUtils.linkSteps(from, toStep);
			});
		});
		return workflow;
	}
}
