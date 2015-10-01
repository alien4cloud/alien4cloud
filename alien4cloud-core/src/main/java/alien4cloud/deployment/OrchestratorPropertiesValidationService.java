package alien4cloud.deployment;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.topology.task.PropertiesTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.TaskLevel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Perform validation of a topology before deployment.
 */
@Service
public class OrchestratorPropertiesValidationService {

    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;

    public PropertiesTask validate(DeploymentTopology deploymentTopology) {
        if (StringUtils.isBlank(deploymentTopology.getOrchestratorId())) {
            return null;
        }

        Map<String, PropertyDefinition> deploymentProperties = orchestratorDeploymentService.getDeploymentPropertyDefinitions(deploymentTopology
                .getOrchestratorId());
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
}
