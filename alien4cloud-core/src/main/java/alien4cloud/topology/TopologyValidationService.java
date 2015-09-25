package alien4cloud.topology;

import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.Workflow;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.topology.task.NodeFiltersTask;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.RequirementsTask;
import alien4cloud.topology.task.SuggestionsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.topology.task.WorkflowTask;
import alien4cloud.topology.validation.NodeFilterValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyAbstractRelationshipValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.topology.validation.TopologyRequirementBoundsValidationServices;
import alien4cloud.utils.services.ConstraintPropertyService;

@Service
@Slf4j
public class TopologyValidationService {
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private MetaPropertiesService metaPropertiesService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private TagService tagService;
    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @Resource
    private TopologyPropertiesValidationService topologyPropertiesValidationService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyAbstractRelationshipValidationService topologyAbstractRelationshipValidationService;
    @Resource
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;
    @Resource
    private NodeFilterValidationService nodeFilterValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    /**
     * Validate if a topology is valid for deployment or not, before deployment configuration
     *
     * @param topology topology to be validated
     * @return the validation result
     */
    public TopologyValidationResult validateTopologyBeforeDeploymentConfig(Topology topology) {
        TopologyValidationResult dto = new TopologyValidationResult();
        if (topology.getNodeTemplates() == null || topology.getNodeTemplates().size() < 1) {
            dto.setValid(false);
            return dto;
        }

        // validate the workflows
        List<WorkflowTask> tasks = Lists.newArrayList();
        if (topology.getWorkflows() != null) {
            TopologyContext topologyContext = workflowBuilderService.buildTopologyContext(topology);
            for (Workflow workflow : topology.getWorkflows().values()) {
                int errorCount = workflowBuilderService.validateWorkflow(topologyContext, workflow);
                if (errorCount > 0) {
                    dto.setValid(false);
                    WorkflowTask workflowTask = new WorkflowTask();
                    workflowTask.setCode(TaskCode.WORKFLOW_INVALID);
                    workflowTask.setWorkflowName(workflow.getName());
                    workflowTask.setErrorCount(errorCount);
                    tasks.add(workflowTask);
                }
            }
        }
        dto.addToTaskList(tasks);

        // validate abstract relationships
        dto.addToTaskList(topologyAbstractRelationshipValidationService.validateAbstractRelationships(topology));

        // validate abstract node types and find suggestions
        dto.addToTaskList(topologyAbstractNodeValidationService.findReplacementForAbstracts(topology));

        // validate requirements lowerBounds
        dto.addToTaskList(topologyRequirementBoundsValidationServices.validateRequirementsLowerBounds(topology));

        // validate the node filters for all relationships
        dto.addToTaskList(nodeFilterValidationService.validateRequirementFilters(topology));

        // FIXME property validation doesn't ignore inputs. It should.
        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        // check also CLOUD / ENVIRONMENT meta properties
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateProperties(topology);

        // List<PropertiesTask> validateProperties = null;
        if (hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addToWarningList(validateProperties);
        } else {
            dto.addToTaskList(validateProperties);
        }

        dto.setValid(isValidTaskList(dto.getTaskList()));

        return dto;
    }

    private boolean hasOnlyPropertiesWarnings(List<PropertiesTask> properties) {
        if (properties == null) {
            return true;
        }
        for (PropertiesTask task : properties) {
            if (CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED))
                    || CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.ERROR))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Define if a tasks list is valid or not regarding task types
     *
     * @param taskList
     * @return
     */
    private boolean isValidTaskList(List<AbstractTask> taskList) {
        if (taskList == null) {
            return true;
        }
        for (AbstractTask task : taskList) {
            // checking SuggestionsTask or RequirementsTask
            if (task instanceof SuggestionsTask || task instanceof RequirementsTask || task instanceof PropertiesTask || task instanceof NodeFiltersTask
                    || task instanceof WorkflowTask) {
                return false;
            }
        }
        return true;
    }
}
