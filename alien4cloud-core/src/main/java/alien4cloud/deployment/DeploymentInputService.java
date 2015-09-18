package alien4cloud.deployment;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Maps;

@Service
public class DeploymentInputService {

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    /**
     * Fill-in the inputs properties definitions (and default values) based on the properties definitions from the topology.
     *
     * @param topology The deployment setup to impact.
     * @param topology The topology that contains the inputs and properties definitions.
     */
    public boolean generateInputProperties(DeploymentTopology topology) {
        Map<String, String> inputProperties = topology.getInputProperties();
        Map<String, PropertyDefinition> inputDefinitions = topology.getInputs();
        boolean changed = false;
        if (inputDefinitions == null || inputDefinitions.isEmpty()) {
            topology.setInputProperties(null);
            changed = inputProperties != null;
        } else {
            if (inputProperties == null) {
                inputProperties = Maps.newHashMap();
                topology.setInputProperties(inputProperties);
                changed = true;
            } else {
                Iterator<Map.Entry<String, String>> inputPropertyEntryIterator = inputProperties.entrySet().iterator();
                while (inputPropertyEntryIterator.hasNext()) {
                    Map.Entry<String, String> inputPropertyEntry = inputPropertyEntryIterator.next();
                    if (!inputDefinitions.containsKey(inputPropertyEntry.getKey())) {
                        inputPropertyEntryIterator.remove();
                        changed = true;
                    } else {
                        try {
                            constraintPropertyService.checkSimplePropertyConstraint(inputPropertyEntry.getKey(), inputPropertyEntry.getValue(),
                                    inputDefinitions.get(inputPropertyEntry.getKey()));
                        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
                            // Property is not valid anymore for the input, remove the old value
                            inputPropertyEntryIterator.remove();
                            changed = true;
                        }
                    }
                }
            }
            for (Map.Entry<String, PropertyDefinition> inputDefinitionEntry : inputDefinitions.entrySet()) {
                String existingValue = inputProperties.get(inputDefinitionEntry.getKey());
                if (existingValue == null) {
                    String defaultValue = inputDefinitionEntry.getValue().getDefault();
                    if (defaultValue != null) {
                        inputProperties.put(inputDefinitionEntry.getKey(), defaultValue);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }
}
