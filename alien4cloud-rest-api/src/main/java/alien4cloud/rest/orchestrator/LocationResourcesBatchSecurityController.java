package alien4cloud.rest.orchestrator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Sets;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
import alien4cloud.rest.orchestrator.model.GroupDTO;
import alien4cloud.rest.orchestrator.model.SubjectsAuthorizationRequest;
import alien4cloud.security.Permission;
import alien4cloud.security.ResourcePermissionService;
import alien4cloud.security.Subject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/" })
@Api(value = "", description = "Location resource security batch operations")
public class LocationResourcesBatchSecurityController {
    @Resource
    private LocationService locationService;
    @Resource
    private LocationSecurityService locationSecurityService;
    @Resource
    private ILocationResourceService locationResourceService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ResourcePermissionService resourcePermissionService;

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    /*******************************************************************************************************************************
     *
     * BATCH SECURITY ON USERS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access on the location resoures to the users (deploy on the location)
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Batch api to grant permissions to multiple users on multiple location resources.", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> grantAccessToUsersOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestParam(required = false, defaultValue = "false") boolean force, @RequestBody SubjectsAuthorizationRequest request) {
        processGrantForSubjectType(Subject.USER, orchestratorId, locationId, force, request);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Revoke authorisations to access the provided resources for the provided users
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Batch api to revoke permissions for multiple users on multiple location resources.", notes = "Only user with ADMIN role can revoke access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> revokeAccessToUsersOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody SubjectsAuthorizationRequest request) {
        processRevokeForSubjectType(Subject.USER, request);
        return RestResponseBuilder.<Void> builder().build();
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON GROUPS
     *
     *******************************************************************************************************************************/

    /**
     * Grant access to the location resource to the groups
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Batch api to revoke permissions for multiple groups on multiple location resources.", notes = "Only user with ADMIN role can grant access to a group.")
    @RequestMapping(value = "/groups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<List<GroupDTO>> grantAccessToGroupsOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestParam(required = false, defaultValue = "false") boolean force, @RequestBody SubjectsAuthorizationRequest request) {
        processGrantForSubjectType(Subject.GROUP, orchestratorId, locationId, force, request);
        return RestResponseBuilder.<List<GroupDTO>> builder().build();
    }

    /**
     * Revoke authorisations to access the provided resources for the provided groups
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Batch api to revoke permissions for multiple groups on multiple location resources.", notes = "Only user with ADMIN role can grant / revoke access to another users.")
    @RequestMapping(value = "/groups", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> revokeAccessToGroupsOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody SubjectsAuthorizationRequest request) {
        processRevokeForSubjectType(Subject.GROUP, request);
        return RestResponseBuilder.<Void> builder().build();
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON APPLICATIONS
     *
     *******************************************************************************************************************************/

    /**
     * Update applications/environments authorized to access the location resource.
     */
    @ApiOperation(value = "Update applications/environments authorized to access the location resource", notes = "Only user with ADMIN role can update authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestParam(required = false, defaultValue = "false") boolean force, @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {

        if (ArrayUtils.isEmpty(request.getResources())) {
            return RestResponseBuilder.<Void> builder().build();
        }

        Location location = locationService.getLocation(orchestratorId, locationId);
        checkAllAuthorizationsForApplicationsAndEnvironments(request, location, force);

        Arrays.stream(request.getResources()).forEach(resourceId -> {
            LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
            if (ArrayUtils.isNotEmpty(request.getApplicationsToDelete())) {
                resourcePermissionService.revokePermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.APPLICATION,
                        request.getApplicationsToDelete());
            }
            if (ArrayUtils.isNotEmpty(request.getEnvironmentsToDelete())) {
                resourcePermissionService.revokePermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                        request.getEnvironmentsToDelete());
            }
            Set<String> envIds = Sets.newHashSet();
            if (ArrayUtils.isNotEmpty(request.getApplicationsToAdd())) {
                resourcePermissionService.grantPermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.APPLICATION,
                        request.getApplicationsToAdd());
                // when an app is added, all eventual existing env authorizations are removed
                for (String applicationToAddId : request.getApplicationsToAdd()) {
                    ApplicationEnvironment[] aes = applicationEnvironmentService.getByApplicationId(applicationToAddId);
                    for (ApplicationEnvironment ae : aes) {
                        envIds.add(ae.getId());
                    }
                }
                if (!envIds.isEmpty()) {
                    resourcePermissionService.revokePermission(resourceTemplate,
                            (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                            envIds.toArray(new String[envIds.size()]));
                }
            }
            if (ArrayUtils.isNotEmpty(request.getEnvironmentsToAdd())) {
                List<String> envToAddSet = Arrays.stream(request.getEnvironmentsToAdd()).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
                resourcePermissionService.grantPermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT,
                        envToAddSet.toArray(new String[envToAddSet.size()]));
            }
        });

        return RestResponseBuilder.<Void> builder().build();
    }

    private void processGrantForSubjectType(Subject subjectType, String orchestratorId, String locationId, boolean grantAccess,
            SubjectsAuthorizationRequest request) {
        if (ArrayUtils.isEmpty(request.getResources())) {
            return;
        }
        Location location = locationService.getLocation(orchestratorId, locationId);
        checkAuthorizations(location, subjectType, grantAccess, request.getSubjects());

        Arrays.stream(request.getResources()).forEach(resourceId -> {
            LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
            // prefer using locationResourceService.saveResource so that the location update date is update.
            // This will then trigger a deployment topology update
            resourcePermissionService.grantPermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), subjectType, request.getSubjects());
        });
    }

    /**
     * Check if all subjects have the authorization on the location .
     *
     * @param location The location on which to check
     * @param subjectType The type of the subjects to check for authorizations
     * @param grantAccess whether or not we want to grant access to unauthorized subjects. Fails with an {@link java.nio.file.AccessDeniedException} if set to
     *            false.
     * @param subjects The subjects to process
     */
    private void checkAuthorizations(Location location, Subject subjectType, boolean grantAccess, String... subjects) {
        Set<String> unauthorized = Arrays.stream(subjects).filter(name -> !location.getPermissions(subjectType, name).contains(Permission.ADMIN))
                .collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(unauthorized)) {
            if (grantAccess) {
                resourcePermissionService.grantPermission(location, subjectType, unauthorized.toArray(new String[unauthorized.size()]));
            } else {
                throw new AccessDeniedException("At least one of the current <" + subjectType + "> has no authorization on location <" + location.getName()
                        + "> to perform the requested operation: " + unauthorized.toString());

            }
        }
    }

    private void processRevokeForSubjectType(Subject subjectType, SubjectsAuthorizationRequest request) {
        if (ArrayUtils.isEmpty(request.getResources())) {
            return;
        }
        Arrays.stream(request.getResources()).forEach(resourceId -> {
            LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
            resourcePermissionService.revokePermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)), subjectType, request.getSubjects());
        });
    }

    private void checkAllAuthorizationsForApplicationsAndEnvironments(ApplicationEnvironmentAuthorizationUpdateRequest request, Location location,
            boolean grantAccess) {
        if (ArrayUtils.isNotEmpty(request.getApplicationsToAdd())) {
            checkAuthorizations(location, Subject.APPLICATION, grantAccess, request.getApplicationsToAdd());
        }
        if (ArrayUtils.isNotEmpty(request.getEnvironmentsToAdd())) {
            checkAuthorizations(location, Subject.ENVIRONMENT, grantAccess, request.getEnvironmentsToAdd());
        }
    }

}
