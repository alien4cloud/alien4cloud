package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Perform validation of a topology before deployment.
 */
@Service
@Slf4j
public class OrchestratorPropertiesValidationService {

    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    public PropertiesTask validate(DeploymentTopology deploymentTopology) {
        if (StringUtils.isBlank(deploymentTopology.getOrchestratorId())) {
            return null;
        }

        Map<String, PropertyDefinition> deploymentProperties = orchestratorDeploymentService.getDeploymentPropertyDefinitions(deploymentTopology
                .getOrchestratorId());
        if (MapUtils.isEmpty(deploymentProperties)) {
            return null;
        }
        Map<String, String> properties = deploymentTopology.getProviderDeploymentProperties();
        if (properties == null) {
            properties = Maps.newHashMap();
        }
        PropertiesTask task = null;
        List<String> required = Lists.newArrayList();

        for (Entry<String, PropertyDefinition> entry : deploymentProperties.entrySet()) {
            if (entry.getValue().isRequired()) {
                String value = properties.get(entry.getKey());
                if (StringUtils.isBlank(value)) {
                    required.add(entry.getKey());
                }
            }
        }

        if (CollectionUtils.isNotEmpty(required)) {
            task = new PropertiesTask(Maps.<TaskLevel, List<String>> newHashMap());
            task.setCode(TaskCode.ORCHESTRATOR_PROPERTY);
            task.getProperties().put(TaskLevel.REQUIRED, required);
        }

        return task;
    }

    public void checkConstraints(String orchestratorId, Map<String, String> properties) throws ConstraintValueDoNotMatchPropertyTypeException,
            ConstraintViolationException {
        if (StringUtils.isBlank(orchestratorId) || MapUtils.isEmpty(properties)) {
            return;
        }
        Map<String, PropertyDefinition> deploymentPropertyDefinitions = orchestratorDeploymentService.getDeploymentPropertyDefinitions(orchestratorId);
        if (MapUtils.isEmpty(deploymentPropertyDefinitions)) {
            throw new NotFoundException("No properties are defined for this orchestrator");
        }

        for (Entry<String, String> propertyEntry : properties.entrySet()) {
            PropertyDefinition propertyDefinition = deploymentPropertyDefinitions.get(propertyEntry.getKey());
            if (propertyDefinition == null) {
                throw new NotFoundException("property <" + propertyEntry.getKey() + "> is not defined for this orchestrator");
            }
            if (propertyDefinition.getConstraints() != null) {
                constraintPropertyService.checkSimplePropertyConstraint(propertyEntry.getKey(), propertyEntry.getValue(), propertyDefinition);
            }
        }
    }
}
