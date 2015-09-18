package alien4cloud.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.topology.Topology;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class DeploymentTopologyValidationService {

    @Inject
    private ConstraintPropertyService constraintPropertyService;

    /**
     * Perform validation of a deployment topology.
     * 
     * @param deploymentTopology The topology to check.
     * @return A TopologyValidationResult with a list of errors and/or warnings.
     */
    public TopologyValidationResult validateTopology(DeploymentTopology deploymentTopology) {
        // TODO Perform validation of the topology inputs

        // TODO Perform validation of policies
        // If a policy is not matched on the location this is a warning as we allow deployment but some features may be missing
        // If a policy requires a configuration or cannot be applied du to any reason the policy implementation itself can trigger some errors (see Orchestrator
        // plugins)
        // FIXME Perform validation of the provider properties!
        TopologyValidationResult validationResult = new TopologyValidationResult();
        validationResult.setValid(true);
        return validationResult;
    }

    /**
     * Perform validation of a given deployment setup against its topology.
     *
     * @param deploymentTopology The deployment setup to validate.
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException
     */
    public void validate(DeploymentTopology deploymentTopology)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        validateInputProperties(deploymentTopology);
        // FIXME Perform validation of the provider properties!
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
