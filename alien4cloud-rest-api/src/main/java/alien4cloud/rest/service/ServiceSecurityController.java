package alien4cloud.rest.service;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.authorization.ResourcePermissionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationDTO;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
import alien4cloud.rest.orchestrator.model.GroupDTO;
import alien4cloud.rest.orchestrator.model.UserDTO;
import alien4cloud.security.Subject;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.elasticsearch.common.collect.Lists;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping({ "/rest/v1/services/{serviceId}/security/", "/rest/latest/services/{serviceId}/security/" })
@Api(value = "", description = "Allow to grant/revoke services authorizations")
public class ServiceSecurityController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ServiceResourceService serviceResourceService;
    @Resource
    private ResourcePermissionService resourcePermissionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;


    /*******************************************************************************************************************************
     *
     * SECURITY ON USERS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access to the location to the user (deploy on the location)
     *
     * @param serviceId The location's id.
     * @param userNames The authorized users.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the service to the users, send back the new authorised users list", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> grantAccessToUsers(@PathVariable String serviceId, @RequestBody String[] userNames) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.grantPermission(service, Subject.USER, userNames);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(service));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * Revoke the user's authorisation to access the location
     *
     * @param serviceId The id of the location.
     * @param username The authorized user.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the user's authorisation to access the service resource, send back the new authorised users list", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/users/{username}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<UserDTO>> revokeUserAccess(@PathVariable String serviceId, @PathVariable String username) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.revokePermission(service, Subject.USER, username);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(service));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }

    /**
     * List all users authorised to access the location.
     *
     * @return list of all users.
     */
    @ApiOperation(value = "List all users authorized to access the service resource", notes = "Only user with ADMIN role can list authorized users to the location.")
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<UserDTO>> getAuthorizedUsers(@PathVariable String serviceId) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        List<UserDTO> users = UserDTO.convert(resourcePermissionService.getAuthorizedUsers(service));
        return RestResponseBuilder.<List<UserDTO>> builder().data(users).build();
    }


    /*******************************************************************************************************************************
     *
     * SECURITY ON GROUPS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access to the service resource to the groups (deploy on the location)
     *
     * @param serviceId The location's id.
     * @param groupIds The authorized groups.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Grant access to the service resource to the groups", notes = "Only user with ADMIN role can grant access to a group.")
    @RequestMapping(value = "/groups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<GroupDTO>> grantAccessToGroups(@PathVariable String serviceId, @RequestBody String[] groupIds) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.grantPermission(service, Subject.GROUP, groupIds);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(service));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * Revoke the group's authorisation to access the location
     *
     * @param serviceId The id of the location.
     * @param groupId The authorized group.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the group's authorisation to access the service resource", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/groups/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<GroupDTO>> revokeGroupAccess(@PathVariable String serviceId, @PathVariable String groupId) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.revokePermission(service, Subject.GROUP, groupId);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(service));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }

    /**
     * List all groups authorised to access the location.
     *
     * @return list of all authorised groups.
     */
    @ApiOperation(value = "List all groups authorized to access the service resource", notes = "Only user with ADMIN role can list authorized groups to the location.")
    @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<GroupDTO>> getAuthorizedGroups(@PathVariable String serviceId) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        List<GroupDTO> groups = GroupDTO.convert(resourcePermissionService.getAuthorizedGroups(service));
        return RestResponseBuilder.<List<GroupDTO>> builder().data(groups).build();
    }


    /*******************************************************************************************************************************
     *
     * SECURITY ON APPLICATIONS
     *
     *******************************************************************************************************************************/

    /**
     * Revoke the application's authorisation to access the location (including all related environments).
     *
     * @param serviceId The id of the location.
     * @param applicationId The authorized application.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Revoke the application's authorisation to access the service resource", notes = "Only user with ADMIN role can revoke access to the location.")
    @RequestMapping(value = "/applications/{applicationId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> revokeApplicationAccess(@PathVariable String serviceId, @PathVariable String applicationId) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.revokePermission(service, Subject.APPLICATION, applicationId);
        // remove all environments related to this application
        ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationId);
        String[] envIds = Arrays.stream(aes).map(ae -> ae.getId()).toArray(String[]::new);
        resourcePermissionService.revokePermission(service, Subject.ENVIRONMENT, envIds);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Update applications/environments authorized to access the location.
     */
    @ApiOperation(value = "Update applications/environments authorized to access the service resource", notes = "Only user with ADMIN role can update authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsPerApplication(@PathVariable String serviceId, @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        resourcePermissionService.revokeAuthorizedEnvironmentsPerApplication(service, request.getApplicationsToDelete(), request.getEnvironmentsToDelete(), request.getEnvironmentTypesToDelete());
        resourcePermissionService.grantAuthorizedEnvironmentsAndEnvTypesPerApplication(service, request.getApplicationsToAdd(), request.getEnvironmentsToAdd(), request.getEnvironmentTypesToAdd());
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * List all environments per application authorised to access the location.
     *
     * @return list of all environments per application.
     */
    @ApiOperation(value = "List all applications/environments authorized to access the service resource", notes = "Only user with ADMIN role can list authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<ApplicationEnvironmentAuthorizationDTO>> getAuthorizedEnvironmentsPerApplication(@PathVariable String serviceId) {
        ServiceResource service = serviceResourceService.getOrFail(serviceId);
        List<Application> applicationsRelatedToEnvironment = Lists.newArrayList();
        List<Application> applicationsRelatedToEnvironmentType = Lists.newArrayList();
        List<ApplicationEnvironment> environments = Lists.newArrayList();
        List<Application> applications = Lists.newArrayList();

        if (service.getEnvironmentPermissions() != null && service.getEnvironmentPermissions().size() > 0) {
            environments = alienDAO.findByIds(ApplicationEnvironment.class, service.getEnvironmentPermissions().keySet().toArray(new String[service.getEnvironmentPermissions().size()]));
            Set<String> environmentApplicationIds = environments.stream().map(ae -> ae.getApplicationId()).collect(Collectors.toSet());
            applicationsRelatedToEnvironment = alienDAO.findByIds(Application.class, environmentApplicationIds.toArray(new String[environmentApplicationIds.size()]));
        }

        if (service.getEnvironmentTypePermissions() != null && service.getEnvironmentTypePermissions().size() > 0) {
            Set<String> environmentTypeApplicationIds = Sets.newHashSet();
            for (String envType : service.getEnvironmentTypePermissions().keySet()) {
                environmentTypeApplicationIds.add(envType.split(":")[0]);
            }
            applicationsRelatedToEnvironmentType = alienDAO.findByIds(Application.class, environmentTypeApplicationIds.toArray(new String[environmentTypeApplicationIds.size()]));
        }

        if (service.getApplicationPermissions() != null && service.getApplicationPermissions().size() > 0) {
            applications = alienDAO.findByIds(Application.class, service.getApplicationPermissions().keySet().toArray(new String[service.getApplicationPermissions().size()]));
        }

        List<ApplicationEnvironmentAuthorizationDTO> result = ApplicationEnvironmentAuthorizationDTO.buildDTOs(applicationsRelatedToEnvironment, applicationsRelatedToEnvironmentType, environments, applications, Lists.newArrayList());
        return RestResponseBuilder.<List<ApplicationEnvironmentAuthorizationDTO>> builder().data(result).build();
    }

}