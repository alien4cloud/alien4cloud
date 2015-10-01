package alien4cloud.topology.validation;

import java.util.List;

import org.springframework.stereotype.Component;

import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.exceptions.LocationRequiredException;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.TaskCode;

import com.google.common.collect.Lists;

/**
 * Performs validation by checking location policies.
 */
@Component
public class LocationPolicyValidationService {

    public List<LocationPolicyTask> validateLocationPolicies(DeploymentTopology deploymentTopology) {
        List<LocationPolicyTask> tasks = null;
        try {
            TopologyLocationUtils.getLocationIdOrFail(deploymentTopology);
        } catch (LocationRequiredException e) {
            // TODO change this later, as now we only support one location policy
            LocationPolicyTask task = new LocationPolicyTask(AlienConstants.GROUP_ALL);
            task.setCode(TaskCode.LOCATION_POLICY);
            tasks = Lists.newArrayList(task);
        }

        return tasks;
    }
}
