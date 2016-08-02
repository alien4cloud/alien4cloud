package alien4cloud.deployment;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidationService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.topology.validation.NodeFilterValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class DeploymentTopologyValidationService {

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
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private LocationPolicyValidationService locationPolicyValidationService;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;
    @Inject
    private DeploymentNodeSubstitutionValidationService substitutionValidationServices;
    @Inject
    private NodeFilterValidationService nodeFilterValidationService;

    /**
     * Perform validation of a deployment topology.
     *
     * @param deploymentTopology The topology to check.
     * @return A DeploymentTopologyValidationResult with a list of errors and/or warnings er steps.
     */
    public TopologyValidationResult validateDeploymentTopology(DeploymentTopology deploymentTopology) {
        TopologyValidationResult dto = new TopologyValidationResult();
        if (deploymentTopology.getNodeTemplates() == null || deploymentTopology.getNodeTemplates().size() < 1) {
            dto.setValid(false);
            return dto;
        }
        // TODO may be include the hoe topology validation here?

        // TODO Perform validation of policies
        // If a policy is not matched on the location this is a warning as we allow deployment but some features may be missing
        // If a policy requires a configuration or cannot be applied du to any reason the policy implementation itself can trigger some errors (see Orchestrator
        // plugins)

        // validate workflows
        dto.addTasks(workflowBuilderService.validateWorkflows(deploymentTopology));

        // validate abstract node types
        dto.addTasks(topologyAbstractNodeValidationService.findReplacementForAbstracts(deploymentTopology));

        // validate substitutions
        dto.addTasks(substitutionValidationServices.validateNodeSubstitutions(deploymentTopology));

        // location policies
        dto.addTasks(locationPolicyValidationService.validateLocationPolicies(deploymentTopology));

        // validate inputs properties
        dto.addTask(validateInputProperties(deploymentTopology));

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        // check also location / ENVIRONMENT meta properties
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateAllProperties(deploymentTopology);

        // validate orchestrator properties
        PropertiesTask orchestratorValidation = orchestratorPropertiesValidationService.validate(deploymentTopology);
        if (orchestratorValidation != null) {
            dto.addTasks(Lists.newArrayList(orchestratorValidation));
        }

        // Validate node filters requirements
        dto.addTasks(nodeFilterValidationService.validateAllRequirementFilters(deploymentTopology));

        if (TopologyValidationService.hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addWarnings(validateProperties);
        } else {
            dto.addTasks(validateProperties);
        }

        dto.setValid(TopologyValidationService.isValidTaskList(dto.getTaskList()));

        return dto;
    }

    /**
     * Validate all required input is provided with a non null value
     *
     * @param deploymentTopology The deployment topology to check.
     * @return A property task with all required missing values or null if all required properties are configured.
     */
    public PropertiesTask validateInputProperties(DeploymentTopology deploymentTopology) {
        if (MapUtils.isEmpty(deploymentTopology.getInputs())) {
            return null;
        }
        // Define a task regarding properties
        PropertiesTask task = new PropertiesTask();
        task.setCode(TaskCode.INPUT_PROPERTY);
        task.setProperties(Maps.<TaskLevel, List<String>> newHashMap());
        task.getProperties().put(TaskLevel.REQUIRED, Lists.<String> newArrayList());
        Map<String, AbstractPropertyValue> inputValues = safe(deploymentTopology.getInputProperties());
        for (Entry<String, PropertyDefinition> propDef : safe(deploymentTopology.getInputs().entrySet())) {
            if (propDef.getValue().isRequired() && inputValues.get(propDef.getKey()) == null) {
                task.getProperties().get(TaskLevel.REQUIRED).add(propDef.getKey());
            }
        }

        return CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED)) ? task : null;
    }
}
