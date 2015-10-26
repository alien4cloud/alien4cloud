package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
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
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.topology.validation.LocationPolicyValidationService;
import alien4cloud.topology.validation.TopologyAbstractNodeValidationService;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

        if (TopologyValidationService.hasOnlyPropertiesWarnings(validateProperties)) {
            dto.addWarnings(validateProperties);
        } else {
            dto.addTasks(validateProperties);
        }

        dto.setValid(TopologyValidationService.isValidTaskList(dto.getTaskList()));

        return dto;
    }

    /**
     * Check that the constraint on an input properties and orchestrator properties is respected for a deployment setup
     *
     * @param topology The deployment topology to validate
     * @param topology The topology that contains the inputs and properties definitions.
     * @throws ConstraintValueDoNotMatchPropertyTypeException
     * @throws ConstraintViolationException
     */
    public void checkPropertiesContraints(DeploymentTopology topology) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        checkInputsContraints(topology);
        orchestratorPropertiesValidationService.checkConstraints(topology.getOrchestratorId(), topology.getProviderDeploymentProperties());
    }

    private void checkInputsContraints(DeploymentTopology topology) throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        if (topology.getInputProperties() == null) {
            return;
        }
        Map<String, String> inputProperties = topology.getInputProperties();
        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
        if (MapUtils.isEmpty(inputDefinitions)) {
            throw new NotFoundException("No input is defined for the topology");
        }
        for (Map.Entry<String, String> inputPropertyEntry : inputProperties.entrySet()) {
            PropertyDefinition definition = inputDefinitions.get(inputPropertyEntry.getKey());
            if (definition != null) {
                constraintPropertyService.checkSimplePropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue(),
                        inputDefinitions.get(inputPropertyEntry.getKey()));
            }
        }
    }

    /**
     *
     * Validate all required input is provided with a non null value
     *
     * @param deploymentTopology
     * @return
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
        Map<String, String> inputValues = Maps.newHashMap();
        if (MapUtils.isNotEmpty(deploymentTopology.getInputProperties())) {
            inputValues = deploymentTopology.getInputProperties();
        }
        for (Entry<String, PropertyDefinition> propDef : deploymentTopology.getInputs().entrySet()) {
            if (propDef.getValue().isRequired()) {
                String value = inputValues.get(propDef.getKey());
                if (StringUtils.isEmpty(value)) {
                    task.getProperties().get(TaskLevel.REQUIRED).add(propDef.getKey());
                }
            }

        }

        return CollectionUtils.isNotEmpty(task.getProperties().get(TaskLevel.REQUIRED)) ? task : null;
    }
}
