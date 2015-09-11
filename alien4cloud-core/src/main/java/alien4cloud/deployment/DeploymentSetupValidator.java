package alien4cloud.deployment;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentSetup;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * 
 */
@Service
public class DeploymentSetupValidator {
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    /**
     * Perform validation of a given deployment setup against its topology.
     *
     * @param deploymentSetup The deployment setup to validate.
     * @param topology The topology against which to validate the inputs.
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException
     * @throws alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException
     */
    public void validate(DeploymentSetup deploymentSetup, Topology topology)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        validateInputProperties(deploymentSetup, topology);
        // FIXME Perform validation of the provider properties!
    }

    private void validateInputProperties(DeploymentSetup deploymentSetup, Topology topology)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (deploymentSetup.getInputProperties() == null) {
            return;
        }
        Map<String, String> inputProperties = deploymentSetup.getInputProperties();
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
