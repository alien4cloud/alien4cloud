package alien4cloud.orchestrators.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.security.ResourceRoleService;
import alien4cloud.security.event.GroupDeletedEvent;
import alien4cloud.security.event.UserDeletedEvent;
import alien4cloud.utils.AlienUtils;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrchestratorSecurityService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private LocationService locationService;
    @Inject
    private ResourceRoleService resourceRoleService;
    @Inject
    private OrchestratorService orchestratorService;

    /**
     * Add a user role on all locations for a given orchestrator
     *
     * @param orchestratorId
     * @param username
     * @param role
     */
    public void addUserRoleOnAllLocations(String orchestratorId, String username, String role) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedUsers().contains(username)) {
            orchestrator.getAuthorizedUsers().add(username);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.addUserRole(location, username, role);
            }
            alienDAO.save(orchestrator);
        }
    }

    /**
     * Remove a user role on all locations for a given orchestrator
     *
     * @param orchestratorId
     * @param username
     * @param role
     */
    public void removeUserRoleOnAllLocations(String orchestratorId, String username, String role) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (orchestrator.getAuthorizedUsers().contains(username)) {
            orchestrator.getAuthorizedUsers().remove(username);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.removeUserRole(location, username, role);
            }
            alienDAO.save(orchestrator);
        }

    }

    /**
     * Add a group role on all locations for a given orchestrator
     *
     * @param orchestratorId
     * @param groupId
     * @param role
     */
    public void addGroupRoleOnAllLocations(String orchestratorId, String groupId, String role) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (!orchestrator.getAuthorizedGroups().contains(groupId)) {
            orchestrator.getAuthorizedGroups().add(groupId);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.addGroupRole(location, groupId, role);
            }
            alienDAO.save(orchestrator);
        }

    }

    /**
     * Remove a group role on all locations for a given orchestrator
     *
     * @param orchestratorId
     * @param groupId
     * @param role
     */
    public void removeGroupRoleOnAllLocations(String orchestratorId, String groupId, String role) {
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        if (orchestrator.getAuthorizedGroups().contains(groupId)) {
            orchestrator.getAuthorizedGroups().remove(groupId);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.removeGroupRole(location, groupId, role);
            }
            alienDAO.save(orchestrator);
        }
    }

    /**
     * Listener for user deleted. Removes the user from all orchestrators he had authorizations on
     *
     * @param event
     */
    @EventListener
    public void userDeletedEventListener(UserDeletedEvent event) {
        List<Orchestrator> orchestrators = orchestratorService.getAll();
        String userName = event.getUser().getUsername();
        Set<Orchestrator> toUpdate = AlienUtils.safe(orchestrators).stream()
                .filter(orchestrator -> AlienUtils.safe(orchestrator.getAuthorizedUsers()).contains(userName)).collect(Collectors.toSet());
        toUpdate.forEach(orchestrator -> orchestrator.getAuthorizedUsers().remove(userName));
        alienDAO.save(toUpdate.toArray(new Orchestrator[toUpdate.size()]));
    }

    /**
     * Listener for group deleted. Removes the group from all orchestrators he had authorizations on
     *
     * @param event
     */
    @EventListener
    public void groupDeletedEventHandler(GroupDeletedEvent event) {
        List<Orchestrator> orchestrators = orchestratorService.getAll();
        String groupId = event.getGroup().getId();
        Set<Orchestrator> toUpdate = AlienUtils.safe(orchestrators).stream()
                .filter(orchestrator -> AlienUtils.safe(orchestrator.getAuthorizedGroups()).contains(groupId)).collect(Collectors.toSet());
        toUpdate.forEach(orchestrator -> orchestrator.getAuthorizedGroups().remove(groupId));
        alienDAO.save(toUpdate.toArray(new Orchestrator[toUpdate.size()]));
    }
}
