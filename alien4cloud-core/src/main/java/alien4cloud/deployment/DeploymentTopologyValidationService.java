package alien4cloud.deployment;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.common.TagService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidationService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;

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

        // TODO Perform validation of policies
        // If a policy is not matched on the location this is a warning as we allow deployment but some features may be missing
        // If a policy requires a configuration or cannot be applied du to any reason the policy implementation itself can trigger some errors (see Orchestrator
        // plugins)

        // validate workflows
        dto.addToTaskList(workflowBuilderService.validateWorkflows(deploymentTopology));

        // validate abstract node types
        dto.addToTaskList(topologyAbstractNodeValidationService.findReplacementForAbstracts(deploymentTopology));

        // validate substitutions
        dto.addToTaskList(substitutionValidationServices.validateNodeSubstitutions(deploymentTopology));

        // location policies
        dto.addToTaskList(locationPolicyValidationService.validateLocationPolicies(deploymentTopology));

        // validate required properties (properties of NodeTemplate, Relationship and Capability)
        // check also location / ENVIRONMENT meta properties
        List<PropertiesTask> validateProperties = topologyPropertiesValidationService.validateAllProperties(deploymentTopology);

        // validate orchestrator properties
        PropertiesTask orchestratorValidation = orchestratorPropertiesValidationService.validate(deploymentTopology);
        if (orchestratorValidation != null) {
            dto.addToTaskList(Lists.newArrayList(orchestratorValidation));
        }

        if (TopologyValidationService.hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addToWarningList(validateProperties);
        } else {
            dto.addToTaskList(validateProperties);
        }

        dto.setValid(TopologyValidationService.isValidTaskList(dto.getTaskList()));

        return dto;
    }

    /**
     * Validate that the input properties is correct for a deployment setup
     *
     * @param topology The deployment topology to validate
     * @param topology The topology that contains the inputs and properties definitions.
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     * @throws ConstraintViolationException
     */
    public void validateInputProperties(DeploymentTopology topology) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (topology.getInputProperties() == null) {
            return;
        }
        Map<String, String> inputProperties = topology.getInputProperties();
        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
        if (inputDefinitions == null) {
            throw new NotFoundException("Validate input but no input is defined for the topology");
        }
        for (Map.Entry<String, String> inputPropertyEntry : inputProperties.entrySet()) {
            PropertyDefinition definition = inputDefinitions.get(inputPropertyEntry.getKey());
            if (definition != null) {
                constraintPropertyService.checkSimplePropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue(),
                        inputDefinitions.get(inputPropertyEntry.getKey()));
            }
        }
    }
}
