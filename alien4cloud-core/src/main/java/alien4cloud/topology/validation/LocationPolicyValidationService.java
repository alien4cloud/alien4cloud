package alien4cloud.topology.validation;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
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
    @Resource
    private LocationSecurityService locationSecurityService;

    public List<LocationPolicyTask> validateLocationPolicies(DeploymentMatchingConfiguration matchingConfiguration) {
        List<LocationPolicyTask> tasks = Lists.newArrayList();
        Location location = null;
        Orchestrator orchestrator = null;

        boolean found = false;
        for (String locationId : safe(matchingConfiguration.getLocationIds()).values()) {
            found = true;
            location = locationService.getOrFail(locationId);
            orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());

            try {
                // if a location already exists, then check the rigths on it
                locationSecurityService.checkAuthorisation(location, matchingConfiguration.getEnvironmentId());
                // check the orchestrator is still enabled
                if (!Objects.equals(orchestrator.getState(), OrchestratorState.CONNECTED)) {
                    UnavailableLocationTask task = new UnavailableLocationTask(location.getName(),
                            orchestrator.getName());
                    task.setCode(TaskCode.LOCATION_DISABLED);
                    tasks.add(task);
                }

            } catch (AccessDeniedException e) {
                UnavailableLocationTask task = new UnavailableLocationTask(location.getName(), orchestrator.getName());
                task.setCode(TaskCode.LOCATION_UNAUTHORIZED);
                tasks.add(task);
            }
        }
        if (!found) {
            tasks.add(new LocationPolicyTask());
        }

        return tasks;
    }
}
