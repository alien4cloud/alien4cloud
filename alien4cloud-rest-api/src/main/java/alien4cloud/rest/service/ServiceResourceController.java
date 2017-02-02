package alien4cloud.rest.service;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.service.model.CreateServiceResourceRequest;
import alien4cloud.rest.service.model.UpdateServiceResourceRequest;
import alien4cloud.service.ServiceResourceService;
import alien4cloud.utils.ReflectionUtil;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.types.NodeType;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by xdegenne on 01/02/2017.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/services", "/rest/v1/services", "/rest/latest/services" })
@Api(value = "Services", description = "Allow to create/read/update/delete and search services.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
public class ServiceResourceController {

    @Resource
    private IToscaTypeSearchService toscaTypeSearchService;

    @Resource
    private ServiceResourceService serviceResourceService;

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new service.", authorizations = { @Authorization("ADMIN") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(@ApiParam(value = "Create service", required = true) @Valid @RequestBody CreateServiceResourceRequest createRequest) {

        NodeType nodeType = toscaTypeSearchService.find(NodeType.class, createRequest.getServiceNodeType(), createRequest.getArchiveVersion());
        if (nodeType == null) {
            throw new NotFoundException(String.format("Node type [%s] doesn't exists with version [%s].", createRequest.getServiceNodeType(), createRequest.getArchiveVersion()));
        }

        String serviceId = serviceResourceService.create(createRequest.getServiceName(), createRequest.getServiceVersion(), nodeType, createRequest.getDeploymentId());

        return RestResponseBuilder.<String> builder().data(serviceId).build();
    }

    @ApiOperation(value = "Get a service from it's id.")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public RestResponse<ServiceResource> get(@ApiParam(value = "Id of the service to get", required = true) @PathVariable String id) {
        // check roles on the requested cloud
        ServiceResource serviceResource = serviceResourceService.getOrFail(id);
        return RestResponseBuilder.<ServiceResource> builder().data(serviceResource).build();
    }

    @ApiOperation(value = "Update the name, version or/and description of an existing service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
                                     @ApiParam(value = "ServiceResource update request, representing the fields to updates and their new values.", required = true) @Valid @NotEmpty @RequestBody UpdateServiceResourceRequest request) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(id);
        String currentName = serviceResource.getName();
        String currentVersion = serviceResource.getVersion();
        ReflectionUtil.mergeObject(request, serviceResource);
        serviceResourceService.ensureUnicityAndSave(serviceResource, currentName, currentVersion);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Add these locationIds to the authorized list of locations for this service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/authorizeLocations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String[]> authorizeLocations(@ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
                                                 @ApiParam(value = "The ids of the locations to authorize for this service.", required = true) @Valid @NotEmpty @RequestBody String[] locationIds) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(id);
        // TODO: ensure all locationIds really exist

        String[] existingIds = serviceResource.getLocationIds();
        Set<String> ids = Sets.newHashSet();
        if (existingIds != null) {
            ids.addAll(Arrays.asList(existingIds));
        }
        ids.addAll(Arrays.asList(locationIds));
        serviceResource.setLocationIds(ids.toArray(new String[ids.size()]));
        serviceResourceService.save(serviceResource);
        return RestResponseBuilder.<String[]> builder().data(serviceResource.getLocationIds()).build();
    }

    @ApiOperation(value = "Remove these locationIds to the authorized list of locations for this service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/revokeLocations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String[]> revokeLocations(@ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
                                                 @ApiParam(value = "The ids of the locations to revoke for this service.", required = true) @Valid @NotEmpty @RequestBody String[] locationIds) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(id);

        String[] existingIds = serviceResource.getLocationIds();
        if (existingIds != null && existingIds.length > 0) {
            Set<String> olds  = Sets.newHashSet(Arrays.asList(existingIds));
            Set<String> removed  = Sets.newHashSet(Arrays.asList(locationIds));
            Set<String> news = olds.stream().filter(l -> !removed.contains(l)).collect(Collectors.toSet());
            serviceResource.setLocationIds(news.toArray(new String[news.size()]));
            serviceResourceService.save(serviceResource);
        }
        return RestResponseBuilder.<String[]> builder().data(serviceResource.getLocationIds()).build();
    }

}
