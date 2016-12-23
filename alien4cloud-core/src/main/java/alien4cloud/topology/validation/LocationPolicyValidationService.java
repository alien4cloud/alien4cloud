package alien4cloud.topology.validation;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.deployment.exceptions.LocationRequiredException;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.DeployerRole;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.UnavailableLocationTask;
import alien4cloud.utils.AlienConstants;

/**
 * Performs validation by checking location policies.
 */
@Component
public class LocationPolicyValidationService {

    @Resource
    private LocationService locationService;
    @Resource
    private OrchestratorService orchestratorService;

    public List<LocationPolicyTask> validateLocationPolicies(DeploymentTopology deploymentTopology) {
        List<LocationPolicyTask> tasks = Lists.newArrayList();
        Location location = null;
        Orchestrator orchestrator = null;
        try {
            Map<String, String> locationIds = TopologyLocationUtils.getLocationIdsOrFail(deploymentTopology);

            String locationId = locationIds.get(AlienConstants.GROUP_ALL);
            location = locationService.getOrFail(locationId);
            orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());

            // if a location already exists, then check the rigths on it
            AuthorizationUtil.checkAuthorizationForLocation(location, DeployerRole.values());

            // check the orchestrator is still enabled

            if (!Objects.equals(orchestrator.getState(), OrchestratorState.CONNECTED)) {
                UnavailableLocationTask task = new UnavailableLocationTask(location.getName(), orchestrator.getName());
                task.setCode(TaskCode.LOCATION_DISABLED);
                tasks.add(task);
            }

        } catch (LocationRequiredException e) {
            // TODO change this later, as now we only support one location policy
            LocationPolicyTask task = new LocationPolicyTask(AlienConstants.GROUP_ALL);
            task.setCode(TaskCode.LOCATION_POLICY);
            tasks.add(task);
        } catch (AccessDeniedException e) {
            UnavailableLocationTask task = new UnavailableLocationTask(location.getName(), orchestrator.getName());
            task.setCode(TaskCode.LOCATION_UNAUTHORIZED);
            tasks.add(task);
        }

        return tasks.isEmpty() ? null : tasks;
    }
}
