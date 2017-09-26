package alien4cloud.rest.orchestrator;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.authorization.ResourcePermissionService;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.locations.services.ILocationResourceService;
import alien4cloud.orchestrators.locations.services.LocationSecurityService;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.orchestrator.model.ApplicationEnvironmentAuthorizationUpdateRequest;
import alien4cloud.rest.orchestrator.model.SubjectsAuthorizationRequest;
import alien4cloud.security.Subject;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ArrayUtils;
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
     * Bulk access on the location resoures to the users (deploy on the location)
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Bulk api to grant/revoke permissions to multiple users on multiple location resources.", notes = "Only user with ADMIN role can grant access to another users.")
    @RequestMapping(value = "/users", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> bulkAccessToUsersOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                        @RequestBody SubjectsAuthorizationRequest request) {
        if (ArrayUtils.isNotEmpty(request.getCreate())) {
            processGrantForSubjectType(Subject.USER, orchestratorId, locationId, request.getResources(), request.getCreate());
        } else if (ArrayUtils.isNotEmpty(request.getDelete())) {
            processRevokeForSubjectType(Subject.USER, request.getResources(), request.getDelete());
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON GROUPS
     *
     *******************************************************************************************************************************/

    /**
     * Bulk access to the location resource to the groups
     *
     * @param locationId The location's id.
     * @return A {@link Void} {@link RestResponse}.
     */
    @ApiOperation(value = "Bulk api to grant/revoke permissions for multiple groups on multiple location resources.", notes = "Only user with ADMIN role can grant access to a group.")
    @RequestMapping(value = "/groups", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public synchronized RestResponse<Void> bulkAccessToGroupsOnResources(@PathVariable String orchestratorId, @PathVariable String locationId,
                                                                         @RequestBody SubjectsAuthorizationRequest request) {
        if (ArrayUtils.isNotEmpty(request.getCreate())) {
            processGrantForSubjectType(Subject.GROUP, orchestratorId, locationId, request.getResources(), request.getCreate());
        } else if (ArrayUtils.isNotEmpty(request.getDelete())) {
            processRevokeForSubjectType(Subject.GROUP, request.getResources(), request.getDelete());
        }
        return RestResponseBuilder.<Void> builder().build();
    }

    /*******************************************************************************************************************************
     *
     * SECURITY ON APPLICATIONS
     *
     *******************************************************************************************************************************/

    /**
     * Update applications, environments and environment types authorized to access the location resource.
     */
    @ApiOperation(value = "Update applications, environments and environment type authorized to access the location resource", notes = "Only user with ADMIN role can update authorized applications/environments for the location.")
    @RequestMapping(value = "/environmentsPerApplication", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    public synchronized RestResponse<Void> updateAuthorizedEnvironmentsPerApplication(@PathVariable String orchestratorId, @PathVariable String locationId,
            @RequestBody ApplicationEnvironmentAuthorizationUpdateRequest request) {

        if (ArrayUtils.isEmpty(request.getResources())) {
            return RestResponseBuilder.<Void> builder().build();
        }

        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.grantAuthorizationOnLocationIfNecessary(request.getApplicationsToAdd(), request.getEnvironmentsToAdd(), request.getEnvironmentTypesToAdd(), location);

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
            if (ArrayUtils.isNotEmpty(request.getEnvironmentTypesToDelete())) {
                resourcePermissionService.revokePermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT_TYPE,
                        request.getEnvironmentTypesToDelete());
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
            if (ArrayUtils.isNotEmpty(request.getEnvironmentTypesToAdd())) {
                List<String> envToAddSet = Arrays.stream(request.getEnvironmentTypesToAdd()).filter(env -> !envIds.contains(env)).collect(Collectors.toList());
                resourcePermissionService.grantPermission(resourceTemplate,
                        (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), Subject.ENVIRONMENT_TYPE,
                        envToAddSet.toArray(new String[envToAddSet.size()]));
            }
        });

        return RestResponseBuilder.<Void> builder().build();
    }

    private void processGrantForSubjectType(Subject subjectType, String orchestratorId, String locationId,
            String[] resources, String[] subjects) {
        if (ArrayUtils.isEmpty(resources)) {
            return;
        }
        Location location = locationService.getLocation(orchestratorId, locationId);
        locationSecurityService.grantAuthorizationOnLocationIfNecessary(location, subjectType, subjects);

        Arrays.stream(resources).forEach(resourceId -> {
            LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
            // prefer using locationResourceService.saveResource so that the location update date is update.
            // This will then trigger a deployment topology update
            resourcePermissionService.grantPermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource(location, (LocationResourceTemplate) resource)), subjectType, subjects);
        });
    }

    private void processRevokeForSubjectType(Subject subjectType, String[] resources, String[] subjects) {
        if (ArrayUtils.isEmpty(resources)) {
            return;
        }
        Arrays.stream(resources).forEach(resourceId -> {
            LocationResourceTemplate resourceTemplate = locationResourceService.getOrFail(resourceId);
            resourcePermissionService.revokePermission(resourceTemplate,
                    (resource -> locationResourceService.saveResource((LocationResourceTemplate) resource)), subjectType, subjects);
        });
    }
}
