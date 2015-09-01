package alien4cloud.orchestrators.services;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.security.ResourceRoleService;

@Service
public class OrchestratorSecurityService {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;
    @Resource
    private LocationService locationService;
    @Resource
    private ResourceRoleService resourceRoleService;
    @Resource
    private OrchestratorService orchestrService;

    /**
     * Add a user role on all locations for a given orchestrator
     *
     * @param orchestratorId
     * @param username
     * @param role
     */
    public void addUserRoleOnAllLocations(String orchestratorId, String username, String role) {
        Orchestrator orchestrator = orchestrService.getOrFail(orchestratorId);
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
        Orchestrator orchestrator = orchestrService.getOrFail(orchestratorId);
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
        Orchestrator orchestrator = orchestrService.getOrFail(orchestratorId);
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
        Orchestrator orchestrator = orchestrService.getOrFail(orchestratorId);
        if (orchestrator.getAuthorizedGroups().contains(groupId)) {
            orchestrator.getAuthorizedGroups().remove(groupId);
            List<Location> locations = locationService.getAll(orchestratorId);
            for (Location location : locations) {
                resourceRoleService.removeGroupRole(location, groupId, role);
            }
            alienDAO.save(orchestrator);
        }
    }
}
