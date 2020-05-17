package alien4cloud.topology.validation;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import com.google.common.collect.Lists;

import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.AbstractPolicy;
import org.alien4cloud.tosca.model.templates.LocationPlacementPolicy;
import org.alien4cloud.tosca.model.templates.NodeGroup;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.topology.task.LocationPolicyTask;
import alien4cloud.topology.task.OrchestratorsLocationsTask;
import alien4cloud.topology.task.TaskCode;
import alien4cloud.topology.task.UnavailableLocationTask;

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
        String previousOrchestratorId = null;
        if (safe(matchingConfiguration.getLocationGroups()).isEmpty()) {
            // If we went there it is probably because the matching configuration was reset by the LocationMatchingModifier
            // otherwise we will have at least one group.
            tasks.add(new LocationPolicyTask());
            return tasks;
        }
        for (NodeGroup nodeGroup : matchingConfiguration.getLocationGroups().values()) {
            if (nodeGroup.getPolicies() == null ){
                tasks.add(new LocationPolicyTask(nodeGroup.getName()));
                continue;
            }

            String locationId =null;
            for (AbstractPolicy p: nodeGroup.getPolicies()) {
                if (p instanceof LocationPlacementPolicy) {
                    locationId = ((LocationPlacementPolicy) p).getLocationId();
                }
            }
            if (locationId == null ){
                tasks.add(new LocationPolicyTask(nodeGroup.getName()));
                continue;
            }
            location = locationService.getOrFail(locationId);
            orchestrator = orchestratorService.getOrFail(location.getOrchestratorId());


            if (previousOrchestratorId!=null && !previousOrchestratorId.equals(location.getOrchestratorId())) {
                OrchestratorsLocationsTask task  = new OrchestratorsLocationsTask(location.getName(), orchestrator.getName());
                task.setCode(TaskCode.LOCATION_POLICY);
                task.setGroupName(nodeGroup.getName());
                tasks.add(task);
                continue;
            }
            previousOrchestratorId = location.getOrchestratorId();

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

        return tasks;
    }
}
