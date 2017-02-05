package alien4cloud.rest.service;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.model.SortedSearchRequest;
import alien4cloud.rest.service.model.CreateServiceResourceRequest;
import alien4cloud.rest.service.model.UpdateServiceResourceRequest;
import alien4cloud.service.ServiceResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller to access services.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/services", "/rest/v1/services", "/rest/latest/services" })
@Api(value = "Services", description = "Allow to create/read/update/delete and search services.", authorizations = { @Authorization("COMPONENTS_MANAGER") })
public class ServiceResourceController {
    @Resource
    private ServiceResourceService serviceResourceService;

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new service.", authorizations = { @Authorization("ADMIN") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Audit
    public RestResponse<String> create(@ApiParam(value = "Create service", required = true) @Valid @RequestBody CreateServiceResourceRequest createRequest) {
        String serviceId = serviceResourceService.create(createRequest.getName(), createRequest.getVersion(), createRequest.getNodeType(),
                createRequest.getNodeTypeVersion());
        return RestResponseBuilder.<String> builder().data(serviceId).build();
    }

    @ApiOperation(value = "List and iterate service resources.", notes = "This API is a simple api to list (with iteration) the service resources. If you need to search with criterias please look at the advanced search API.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public RestResponse<GetMultipleDataResult<ServiceResource>> list(
            @ApiParam(value = "Optional pagination start index.", defaultValue = "0") @RequestParam(required = false, defaultValue = "0") int from,
            @ApiParam(value = "Optional pagination element count (limited to 1000).", defaultValue = "100") @RequestParam(required = false, defaultValue = "100") int count) {
        if (count > 1000) {
            throw new InvalidArgumentException("Count cannot be higher than 1000");
        }
        GetMultipleDataResult<ServiceResource> result = serviceResourceService.list(from, count);
        return RestResponseBuilder.<GetMultipleDataResult<ServiceResource>> builder().data(result).build();
    }

    @ApiOperation(value = "Get a service from it's id.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public RestResponse<ServiceResource> get(@ApiParam(value = "Id of the service to get", required = true) @PathVariable String id) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(id);
        return RestResponseBuilder.<ServiceResource> builder().data(serviceResource).build();
    }

    @ApiOperation(value = "Update a service. Note: alien managed services (through application deployment) cannot be updated via API.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> update(@ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "ServiceResource update request, representing the fields to updates and their new values.", required = true) @Valid @NotEmpty @RequestBody UpdateServiceResourceRequest request) {
        serviceResourceService.update(id, request.getName(), request.getVersion(), request.getDescription(), request.getNodeInstance(),
                request.getLocationIds());
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Delete a service. Note: alien managed services (through application deployment) cannot be deleted via API.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the service to delete.", required = true) @PathVariable @Valid @NotEmpty String id) {
        serviceResourceService.delete(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    @ApiOperation(value = "Search services.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/adv/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<GetMultipleDataResult<ServiceResource>> search(@RequestBody SortedSearchRequest searchRequest) {
        GetMultipleDataResult<ServiceResource> result = serviceResourceService.search(searchRequest.getQuery(), searchRequest.getFilters(),
                searchRequest.getSortField(), searchRequest.isDesc(), searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult<ServiceResource>> builder().data(result).build();
    }

    @ApiOperation(value = "Add these locationIds to the authorized list of locations for this service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/adv/{id}/authorizeLocations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String[]> authorizeLocations(
            @ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "The ids of the locations to authorize for this service.", required = true) @Valid @NotEmpty @RequestBody String[] locationIds) {
        String[] updatedLocations = serviceResourceService.addLocations(id, locationIds);
        return RestResponseBuilder.<String[]> builder().data(updatedLocations).build();
    }

    @ApiOperation(value = "Remove these locationIds to the authorized list of locations for this service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/adv/{id}/revokeLocations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<String[]> revokeLocations(@ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "The ids of the locations to revoke for this service.", required = true) @Valid @NotEmpty @RequestBody String[] locationIds) {
        String[] updatedLocations = serviceResourceService.removeLocations(id, locationIds);
        return RestResponseBuilder.<String[]> builder().data(updatedLocations).build();
    }
}