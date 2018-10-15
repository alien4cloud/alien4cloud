package alien4cloud.rest.application;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import alien4cloud.dao.ESGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchFacet;
import alien4cloud.model.deployment.Deployment;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.model.templates.Topology;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteDeployedException;
import alien4cloud.images.IImageDAO;
import alien4cloud.images.exception.ImageUploadException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.rest.application.model.CreateApplicationRequest;
import alien4cloud.rest.application.model.UpdateApplicationRequest;
import alien4cloud.rest.model.*;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.security.model.Role;
import alien4cloud.utils.VersionUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

/**
 * Service that allows managing applications.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/applications", "/rest/v1/applications", "/rest/latest/applications" })
@Api(value = "", description = "Operations on Applications")
public class ApplicationController {
    @Resource
    private IImageDAO imageDAO;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    ArchiveIndexer archiveIndexer;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    /**
     * Create a new application in the system.
     *
     * @param request The new application to create.
     */
    @ApiOperation(value = "Create a new application in the system.", notes = "If successfull returns a rest response with the id of the created application in data. If not successful a rest response with an error content is returned. Role required [ APPLICATIONS_MANAGER ]. "
            + "By default the application creator will have application roles [APPLICATION_MANAGER, DEPLOYMENT_MANAGER]")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<String> create(@Valid @RequestBody CreateApplicationRequest request) {
        AuthorizationUtil.checkHasOneRoleIn(Role.APPLICATIONS_MANAGER);
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // check the topology template id to recover the related topology id
        String topologyId = request.getTopologyTemplateVersionId();
        Topology template = null;
        if (topologyId != null) {
            template = applicationVersionService.getTemplateTopology(topologyId);
        }

        // check unity of archive name
        try {
            archiveIndexer.ensureUniqueness(request.getArchiveName(), VersionUtil.DEFAULT_VERSION_NAME);
        } catch (AlreadyExistException e) {
            return RestResponseBuilder.<String> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.APPLICATION_CSAR_VERSION_ALREADY_EXIST).message(
                            "CSAR: " + request.getArchiveName() + ", Version: " + VersionUtil.DEFAULT_VERSION_NAME + " already exists in the repository.")
                            .build())
                    .build();
        }

        // create the application with default environment and version
        String applicationId = applicationService.create(auth.getName(), request.getArchiveName(), request.getName(), request.getDescription(), template);
        ApplicationVersion version = applicationVersionService.createInitialVersion(applicationId, topologyId);
        applicationEnvironmentService.createApplicationEnvironment(auth.getName(), applicationId, version.getTopologyVersions().keySet().iterator().next());
        return RestResponseBuilder.<String> builder().data(applicationId).build();
    }

    /**
     * Get an application from it's id.
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Application> get(@PathVariable String applicationId) {
        return RestResponseBuilder.<Application> builder().data(applicationService.checkAndGetApplication(applicationId)).build();
    }

    /**
     * Search for an application.
     *
     * @param searchRequest The element that contains criterias for search operation.
     * @return A rest response that contains a {@link FacetedSearchResult} containing applications.
     */
    @ApiOperation(value = "Search for applications", notes = "Returns a search result with that contains applications matching the request. A application is returned only if the connected user has at least one application role in [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<FacetedSearchResult> search(@RequestBody FilteredSearchRequest searchRequest) {
        FilterBuilder authorizationFilter = AuthorizationUtil.getResourceAuthorizationFilters();

        // We want to sort applications by deployed/undeployed and then application name.

        FacetedSearchResult<Application> facetedSearchResult = alienDAO.facetedSearch(Application.class, searchRequest.getQuery(), searchRequest.getFilters(), authorizationFilter, "", 0, Integer.MAX_VALUE);

        // Get their status (deployed vs undeployed)
        List<DeployedAppHolder> appHolders = Lists.newLinkedList();
        for (Application hit : facetedSearchResult.getData()) {
            String id = hit.getId();
            String appName = hit.getName();
            boolean isDeployed = alienDAO.buildQuery(Deployment.class).setFilters(fromKeyValueCouples("sourceId", id, "endDate", null)).count() > 0;
            appHolders.add(new DeployedAppHolder(id, appName, isDeployed));
        }

        // Sort to have first all deployed apps sorted by name and then all undeployed apps sorted by name.
        Collections.sort(appHolders);

        // Compute the list of app ids to fetch based on the query pagination parameters
        List<String> appIdsToFetch = Lists.newArrayList();
        int to = searchRequest.getFrom() + searchRequest.getSize();
        for (int i = searchRequest.getFrom(); i < appHolders.size() && i < to; i++) {
            appIdsToFetch.add(appHolders.get(i).appId);
        }
        List<Application> applications;
        if (appIdsToFetch.size() == 0) {
            applications = Lists.newArrayList();
        } else {
            applications = alienDAO.findByIds(Application.class, appIdsToFetch.toArray(new String[appIdsToFetch.size()]));
        }

        return RestResponseBuilder.<FacetedSearchResult> builder()
                .data(new FacetedSearchResult<>(searchRequest.getFrom(), to, facetedSearchResult.getQueryDuration(), appHolders.size(),
                        new String[] { Application.class.getSimpleName() }, applications.toArray(new Application[applications.size()]), facetedSearchResult.getFacets()))
                .build();
    }

    @AllArgsConstructor
    private class DeployedAppHolder implements Comparable<DeployedAppHolder> {
        private String appId;
        private String appName;
        private boolean isDeployed;

        @Override
        public int compareTo(DeployedAppHolder o) {
            if (this.isDeployed == o.isDeployed) {
                // Both are deployed or not deployed so compare on name
                return appName.toLowerCase().compareTo(o.appName.toLowerCase());
            }
            if (this.isDeployed) {
                return -1; // I am deployed so other is not (see previous if). smaller (first in sorted list).
            }
            return 1;
        }
    }

    /**
     * Delete an application based on it's id.
     *
     * @param applicationId The id of the application to delete.
     * @return A rest response.
     */
    @ApiOperation(value = "Delete an application from its id.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Boolean> delete(@PathVariable String applicationId) {
        applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);

        try {
            boolean deleted = applicationService.delete(applicationId);
            if (!deleted) {
                throw new DeleteDeployedException(
                        "Application with id <" + applicationId + "> cannot be deleted since one of its environment is still deployed.");
            }
        } catch (OrchestratorDisabledException e) {
            log.error("Failed to delete the application due to Cloud error", e);
            return RestResponseBuilder.<Boolean> builder().data(false).error(RestErrorBuilder.builder(RestErrorCode.CLOUD_DISABLED_ERROR)
                    .message("Could not delete the application with id <" + applicationId + "> with error : " + e.getMessage()).build()).build();

        }
        return RestResponseBuilder.<Boolean> builder().data(true).build();
    }

    /**
     * Update application's image.
     *
     * @param applicationId The application id.
     * @param image new image of the application
     * @return nothing if success, error will be handled in global exception strategy
     */
    @ApiOperation(value = "Updates the image for the application.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}/image", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<String> updateImage(@PathVariable String applicationId, @RequestParam("file") MultipartFile image) {
        Application application = applicationService.checkAndGetApplication(applicationId, ApplicationRole.APPLICATION_MANAGER);
        String imageId;
        try {
            imageId = imageDAO.writeImage(image.getBytes());
        } catch (IOException e) {
            throw new ImageUploadException(
                    "Unable to read image from file upload [" + image.getOriginalFilename() + "] to update application [" + applicationId + "]", e);
        }
        application.setImageId(imageId);
        alienDAO.save(application);
        return RestResponseBuilder.<String> builder().data(imageId).build();
    }

    /**
     * Update application's name
     *
     * @param applicationId The application id.
     * @return nothing if success, error will be handled in global exception strategy
     */
    @ApiOperation(value = "Updates by merging the given request into the given application .", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Audit
    public RestResponse<Void> update(@PathVariable String applicationId, @RequestBody UpdateApplicationRequest request) {
        applicationService.update(applicationId, request.getName(), request.getDescription());
        return RestResponseBuilder.<Void> builder().build();
    }

}
