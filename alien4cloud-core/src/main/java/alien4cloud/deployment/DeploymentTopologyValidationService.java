package alien4cloud.deployment;

import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.OrchestratorDeploymentProperties;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidationService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.validation.NodeFilterValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.topology.validation.TopologyServiceInterfaceOverrideCheckerService;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class DeploymentTopologyValidationService {
    @Resource
    private TopologyPropertiesValidationService topologyPropertiesValidationService;
    @Resource
    private TopologyAbstractNodeValidationService topologyAbstractNodeValidationService;
    @Resource
    private WorkflowsBuilderService workflowBuilderService;
    @Inject
    private OrchestratorPropertiesValidationService orchestratorPropertiesValidationService;
    @Inject
    private NodeFilterValidationService nodeFilterValidationService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private TopologyServiceInterfaceOverrideCheckerService topologyServiceInterfaceOverrideCheckerService;

    /**
     * Perform validation of a deployment topology where all inputs have been processed.
     * 
     * @param topology The deployment topology to check.
     * @return A DeploymentTopologyValidationResult with a list of errors and/or warnings er steps.
     */
    public TopologyValidationResult validateProcessedDeploymentTopology(Topology topology, DeploymentMatchingConfiguration matchingConfiguration,
            OrchestratorDeploymentProperties orchestratorDeploymentProperties) {
        TopologyValidationResult dto = new TopologyValidationResult();
        if (topology.getNodeTemplates() == null || topology.getNodeTemplates().size() < 1) {
            dto.setValid(false);
            return dto;
        }

        // Node filters validation is performed twice, once at the editor times to check that the edition does not contains any values that breaks filters
        // And one post matching (below) after all properties are filled-in (inputs and location provided).
        dto.addTasks(nodeFilterValidationService.validateAllRequirementFilters(topology));

        // Display warning if operations from the topology are overriden by service provided operations.
        dto.addWarnings(topologyServiceInterfaceOverrideCheckerService.findWarnings(topology));

        // validate workflows
        dto.addTasks(workflowBuilderService.validateWorkflows(topology));

        // validate abstract node types
        dto.addTasks(topologyAbstractNodeValidationService.findReplacementForAbstracts(topology, matchingConfiguration.getMatchedLocationResources()));

        // TODO Perform here validation of policies
        // If a policy is not matched on the location this is a warning as we allow deployment but some features may be missing
        // If a policy requires a configuration or cannot be applied du to any reason the policy implementation itself can trigger some errors (see Orchestrator
        // plugins)

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        // check also location / ENVIRONMENT meta properties
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateAllProperties(topology);

        // validate orchestrator properties
        PropertiesTask orchestratorValidation = orchestratorPropertiesValidationService.validate(orchestratorDeploymentProperties);
        if (orchestratorValidation != null) {
            dto.addTasks(Lists.newArrayList(orchestratorValidation));
        }

        if (TopologyValidationService.hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addWarnings(validateProperties);
        } else {
            dto.addTasks(validateProperties);
        }

        dto.setValid(TopologyValidationService.isValidTaskList(dto.getTaskList()));

        return dto;
    }
}
