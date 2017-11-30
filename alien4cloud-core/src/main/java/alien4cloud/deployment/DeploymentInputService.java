package alien4cloud.deployment;

import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.utils.PropertyUtil;
import alien4cloud.utils.services.ConstraintPropertyService;
import com.google.common.collect.Maps;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Map;

import static alien4cloud.utils.AlienUtils.safe;

@Service
public class DeploymentInputService {
    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;

    /**
     * Ensure that the specified input values matches the eventually updated input definitions.
     * 
     * @param inputDefinitions Inputs definitions as specified in the topology.
     * @param inputValues Input properties values as specified by the user.
     * @return true if there is an update on inputValues (removal or addition). false if nothing has changed
     */
    public boolean synchronizeInputs(Map<String, PropertyDefinition> inputDefinitions, Map<String, AbstractPropertyValue> inputValues) {
        boolean updated = false;
        if (!MapUtils.isEmpty(inputValues)) {
            // Ensure that previous defined values are still compatible with the latest input definition (as the topology may have changed).
            Iterator<Map.Entry<String, AbstractPropertyValue>> inputPropertyEntryIterator = inputValues.entrySet().iterator();
            while (inputPropertyEntryIterator.hasNext()) {
                Map.Entry<String, AbstractPropertyValue> inputPropertyEntry = inputPropertyEntryIterator.next();
                // remove if the value is null, or the input is not register as one
                if (inputPropertyEntry.getValue() == null || !safe(inputDefinitions).containsKey(inputPropertyEntry.getKey())) {
                    inputPropertyEntryIterator.remove();
                } else if(! (inputPropertyEntry.getValue() instanceof FunctionPropertyValue)) {
                    try {
                        ConstraintPropertyService.checkPropertyConstraint(inputPropertyEntry.getKey(), ((PropertyValue)inputPropertyEntry.getValue()).getValue(),
                                inputDefinitions.get(inputPropertyEntry.getKey()));
                    } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                        // Property is not valid anymore for the input, remove the old value
                        inputPropertyEntryIterator.remove();
                        updated = true;
                    }
                }
            }
        }
        // set default values for every unset property.
        for (Map.Entry<String, PropertyDefinition> inputDefinitionEntry : safe(inputDefinitions).entrySet()) {
            AbstractPropertyValue existingValue = inputValues.get(inputDefinitionEntry.getKey());
            if (existingValue == null) {
                // If user has not specified a value and there is
                PropertyValue defaultValue = inputDefinitionEntry.getValue().getDefault();
                if (defaultValue != null) {
                    inputValues.put(inputDefinitionEntry.getKey(), defaultValue);
                    updated = true;
                }
            }
        }

        return updated;
    }

    /**
     * Process default deployment properties
     *
     * @param deploymentTopology the deployment setup to generate configuration for
     */
    public void processProviderDeploymentProperties(DeploymentTopology deploymentTopology) {
        if (deploymentTopology.getOrchestratorId() == null) {
            // No orchestrator assigned for the topology do nothing
            return;
        }
        Map<String, PropertyDefinition> propertyDefinitionMap = orchestratorDeploymentService
                .getDeploymentPropertyDefinitions(deploymentTopology.getOrchestratorId());
        if (propertyDefinitionMap != null) {
            // Reset deployment properties as it might have changed between orchestrators
            Map<String, String> propertyValueMap = deploymentTopology.getProviderDeploymentProperties();
            if (propertyValueMap == null) {
                propertyValueMap = Maps.newHashMap();
            } else {
                Iterator<Map.Entry<String, String>> propertyValueMapIterator = propertyValueMap.entrySet().iterator();
                while (propertyValueMapIterator.hasNext()) {
                    Map.Entry<String, String> entry = propertyValueMapIterator.next();
                    if (!propertyDefinitionMap.containsKey(entry.getKey())) {
                        // Remove the mapping if topology do not contain the node with that name and of type compute
                        // Or the mapping do not exist anymore in the match result
                        propertyValueMapIterator.remove();
                    }
                }
            }
            for (Map.Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitionMap.entrySet()) {
                String existingValue = propertyValueMap.get(propertyDefinitionEntry.getKey());
                if (existingValue != null) {
                    try {
                        ConstraintPropertyService.checkPropertyConstraint(propertyDefinitionEntry.getKey(), existingValue, propertyDefinitionEntry.getValue());
                    } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                        PropertyUtil.setScalarDefaultValueOrNull(propertyValueMap, propertyDefinitionEntry.getKey(),
                                propertyDefinitionEntry.getValue().getDefault());
                    }
                } else {
                    PropertyUtil.setScalarDefaultValueIfNotNull(propertyValueMap, propertyDefinitionEntry.getKey(),
                            propertyDefinitionEntry.getValue().getDefault());
                }
            }
            deploymentTopology.setProviderDeploymentProperties(propertyValueMap);
        }
    }
}