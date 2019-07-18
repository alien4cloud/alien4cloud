package alien4cloud.topology;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;

import javax.annotation.Resource;

import alien4cloud.topology.task.*;
import alien4cloud.topology.validation.*;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.context.ToscaContextual;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TopologyValidationService {
    @Resource
    private TopologyPropertiesValidationService topologyPropertiesValidationService;
    @Resource
    private TopologyRequirementBoundsValidationServices topologyRequirementBoundsValidationServices;
    @Resource
    private TopologyAbstractRelationshipValidationService topologyAbstractRelationshipValidationService;
    @Resource
    private NodeFilterValidationService nodeFilterValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Resource
    private TopologyArtifactsValidationService topologyArtifactsValidationService;
    @Resource
    private DeprecatedNodeTypesValidationService deprecatedNodeTypesValidationService;
    @Resource
    private TopologyPluginValidationService topologyPluginValidationService;

    /**
     * Validate if a topology is valid for deployment configuration or not,
     * This is done before deployment configuration
     *
     * @param topology topology to be validated
     * @return the validation result
     */
    @ToscaContextual
    public TopologyValidationResult validateTopology(Topology topology) {
        TopologyValidationResult dto = doValidate(topology);
        // set the source of the tasks to know that they are related to validation of the source topology and not deployment topology
        addSource(dto.getTaskList());
        addSource(dto.getWarningList());
        addSource(dto.getInfoList());
        return dto;
    }

    private TopologyValidationResult doValidate(Topology topology) {
        TopologyValidationResult dto = new TopologyValidationResult();
        if (MapUtils.isEmpty(topology.getNodeTemplates())) {
            dto.addTask(new EmptyTask());
            dto.setValid(false);
            return dto;
        }

        topologyPluginValidationService.validate(dto,topology);

        // validate the workflows
        dto.addTasks(workflowBuilderService.validateWorkflows(topology));

        // validate abstract relationships
        dto.addTasks(topologyAbstractRelationshipValidationService.validateAbstractRelationships(topology));

        // validate requirements lowerBounds
        dto.addTasks(topologyRequirementBoundsValidationServices.validateRequirementsLowerBounds(topology));

        // validate the node filters for all relationships
        dto.addTasks(nodeFilterValidationService.validateStaticRequirementFilters(topology));

        // validate that all artifacts has been filled
        dto.addTasks(topologyArtifactsValidationService.validate(topology));

        // Add warning for deprecated nodes.
        dto.addWarnings(deprecatedNodeTypesValidationService.validate(topology));

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateStaticProperties(topology);

        // List<PropertiesTask> validateProperties = null;
        if (hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addWarnings(validateProperties);
        } else {
            dto.addTasks(validateProperties);
        }

        dto.setValid(isValidTaskList(dto.getTaskList()));

        return dto;
    }

    private void addSource(List<AbstractTask> tasks) {
        safe(tasks).forEach(abstractTask -> abstractTask.setSource("topology"));
    }

    public static boolean hasOnlyPropertiesWarnings(List<PropertiesTask> properties) {
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
    public static boolean isValidTaskList(List<AbstractTask> taskList) {
        if (taskList == null) {
            return true;
        }
        for (AbstractTask task : taskList) {
            // checking some required tasks
            if (task instanceof SuggestionsTask || task instanceof RequirementsTask || task instanceof PropertiesTask || task instanceof NodeFiltersTask
                    || task instanceof WorkflowTask || task instanceof ArtifactTask || task instanceof InputArtifactTask
                    || task instanceof AbstractRelationshipTask || task instanceof PluginLogTask) {
                return false;
            }
        }
        return true;
    }
}
