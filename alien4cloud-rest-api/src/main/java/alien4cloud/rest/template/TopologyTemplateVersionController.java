package alien4cloud.rest.template;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.csar.services.CsarService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.DeleteLastApplicationVersionException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.application.ApplicationVersionRequest;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyTemplateVersionService;
import alien4cloud.utils.ReflectionUtil;
import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.UpdateApplicationVersionException;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/rest/templates/{topologyTemplateId:.+}/versions")
@Api(value = "", description = "Manages templates's versions")
public class TopologyTemplateVersionController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyTemplateVersionService versionService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyService topologyService;
    @Resource
    private CsarService csarService;

    /**
     * Get the latest version for a topology template.
     */
    @ApiOperation(value = "Get the latest version for a topology template.")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyTemplateVersion> get(@PathVariable String topologyTemplateId) {
        TopologyTemplateVersion[] versions = versionService.getByDelegateId(topologyTemplateId);
        for (TopologyTemplateVersion current : versions) {
            if (current.isLatest()) {
                return RestResponseBuilder.<TopologyTemplateVersion> builder().data(current).build();
            }
        }
        throw new NotFoundException("No latest verion can be found for topology template " + topologyTemplateId);
    }

    /**
     * Search topology template versions for a given topology template id
     *
     * @param applicationId the targeted application id
     * @param searchRequest
     * @return A rest response that contains a {@link FacetedSearchResult} containing application versions for an application id sorted by version
     */
    @ApiOperation(value = "Search topology template versions", notes = "Returns a search result with that contains application versions matching the request.")
    @RequestMapping(value = "/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<GetMultipleDataResult<TopologyTemplateVersion>> search(@PathVariable String topologyTemplateId, @RequestBody SearchRequest searchRequest) {
        GetMultipleDataResult<TopologyTemplateVersion> searchResult = alienDAO.search(TopologyTemplateVersion.class, null,
                versionService.getVersionsFilters(topologyTemplateId, searchRequest.getQuery()), searchRequest.getFrom(), searchRequest.getSize());
        searchResult.setData(versionService.sortArrayOfVersion(searchResult.getData()));
        return RestResponseBuilder.<GetMultipleDataResult<TopologyTemplateVersion>> builder().data(searchResult).build();
    }

    /**
     * Get topology template version from it's id.
     *
     * @param applicationId The application id
     */
    @ApiOperation(value = "Get an topology template version based from its id.", notes = "Returns the topology template version details. Role required [ TBD ]")
    @RequestMapping(value = "/{versionId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyTemplateVersion> getVersion(@PathVariable String topologyTemplateId, @PathVariable String versionId) {
        TopologyTemplateVersion version = versionService.getOrFail(versionId);
        return RestResponseBuilder.<TopologyTemplateVersion> builder().data(version).build();
    }

    /**
     * Create a new topology template version.
     *
     * @param request data to create an application environment
     * @return application environment id
     */
    @ApiOperation(value = "Create a new topology template version.", notes = "If successfull returns a rest response with the id of the created version in data. If not successful a rest response with an error content is returned. Role required [ ARCHITECT ]. ")
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(value = HttpStatus.CREATED)
    @Audit
    public RestResponse<String> create(@PathVariable String topologyTemplateId, @RequestBody ApplicationVersionRequest request) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplateVersion version = versionService.createVersion(topologyTemplateId, request.getTopologyId(), request.getVersion(),
                request.getDescription(), null);
        Topology topology = topologyServiceCore.getTopology(version.getTopologyId());
        topologyServiceCore.updateSubstitutionType(topology);
        return RestResponseBuilder.<String> builder().data(version.getId()).build();
    }

    /**
     * Update application version
     *
     * @param applicationVersionId
     * @param request
     * @return
     */
    @ApiOperation(value = "Updates by merging the given request into the given topology template version", notes = "Updates by merging the given request into the given topology template version. The logged-in user must have the architect role for this application. Application role required [ ARCHITECT ]")
    @RequestMapping(value = "/{versionId:.+}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Void> update(@PathVariable String versionId, @RequestBody ApplicationVersionRequest request) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplateVersion appVersion = versionService.getOrFail(versionId);

        if (appVersion.isReleased()) {
            throw new UpdateApplicationVersionException("The topology template version " + appVersion.getId() + " is released and cannot be update.");
        } else if (request.getVersion() != null && !VersionUtil.isValid(request.getVersion())) {
            throw new InvalidVersionException(request.getVersion() + "is not a valid version name");
        } else if (request.getVersion() != null && !appVersion.getVersion().equals(request.getVersion())
                && versionService.isVersionNameExist(appVersion.getDelegateId(), request.getVersion())) {
            throw new AlreadyExistException("An topology template version already exist for this topology template with the version :" + versionId);
        }

        if (request.getVersion() != null) {
            appVersion.setSnapshot(VersionUtil.isSnapshot(request.getVersion()));
            appVersion.setReleased(!appVersion.isSnapshot());
        }

        ReflectionUtil.mergeObject(request, appVersion);
        alienDAO.save(appVersion);
        return RestResponseBuilder.<Void> builder().build();
    }

    /**
     * Delete an application environment based on it's id. Should not be able to delete a deployed version.
     *
     * @param applicationId
     * @param applicationVersionId
     * @return boolean is delete
     */
    @ApiOperation(value = "Delete an topology template version from its id", notes = "The logged-in user must have the architect role. Role required [ ARCHITECT ]")
    @RequestMapping(value = "/{versionId:.+}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Audit
    public RestResponse<Boolean> delete(@PathVariable String topologyTemplateId, @PathVariable String versionId) {
        AuthorizationUtil.checkHasOneRoleIn(Role.ARCHITECT);
        TopologyTemplateVersion ttv = versionService.getOrFail(versionId);
        if (versionService.getByDelegateId(topologyTemplateId).length == 1) {
            throw new DeleteLastApplicationVersionException("Topology Template version <" + ttv.getVersion()
                    + "> can't be be deleted beacause it's the last application version.");
        }
        versionService.delete(versionId);
        return RestResponseBuilder.<Boolean> builder().data(true).build();
    }

}
