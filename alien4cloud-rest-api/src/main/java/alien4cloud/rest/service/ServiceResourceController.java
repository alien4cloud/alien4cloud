package alien4cloud.rest.service;

import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.groups.Default;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.model.application.Application;
import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import alien4cloud.rest.service.model.PatchServiceResourceRequest;
import alien4cloud.rest.service.model.UpdateServiceResourceRequest;
import alien4cloud.rest.service.model.UpdateValidationGroup;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.utils.RestConstraintValidator;
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
@Api(value = "Services", description = "Allow to create/read/update/delete and search services.", authorizations = { @Authorization("ADMIN") })
public class ServiceResourceController {
    @Resource
    private ServiceResourceService serviceResourceService;

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Create a new service.", authorizations = { @Authorization("ADMIN") })
    @ResponseStatus(value = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    @Audit
    public RestResponse<ServiceResource> create(@ApiParam(value = "Create service", required = true) @Valid @RequestBody CreateServiceResourceRequest createRequest) {
        String serviceId = serviceResourceService.create(createRequest.getName(), createRequest.getVersion(), createRequest.getNodeType(),
                createRequest.getNodeTypeVersion());
        ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);
        return RestResponseBuilder.<ServiceResource> builder().data(serviceResource).build();
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

    @ApiOperation(value = "Update a service.", notes = "Alien managed services (through application deployment) cannot be updated via API.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintInformation> update(
            @ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "ServiceResource update request, representing the fields to updates and their new values.", required = true) @Validated(value = {
                    Default.class, UpdateValidationGroup.class }) @NotEmpty @RequestBody UpdateServiceResourceRequest request) {
        try {
            serviceResourceService.update(id, request.getName(), request.getVersion(), request.getDescription(), request.getNodeInstance().getType(),
                    request.getNodeInstance().getTypeVersion(), request.getNodeInstance().getProperties(), request.getNodeInstance().getCapabilities(),
                    request.getNodeInstance().getAttributeValues(), request.getLocationIds(), request.getCapabilitiesRelationshipTypes(),
                    request.getRequirementsRelationshipTypes());
            return RestResponseBuilder.<ConstraintInformation> builder().build();
        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
            return RestConstraintValidator.fromException(e, e.getConstraintInformation().getName(), e.getConstraintInformation().getValue());
        }
    }

    @ApiOperation(value = "Patch a service.", notes = "When the service is managed by alien (through application deployment) the only authorized patch are on location and authorizations.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintInformation> patch(
            @ApiParam(value = "Id of the service to update.", required = true) @PathVariable @Valid @NotEmpty String id,
            @ApiParam(value = "ServiceResource patch request, representing the fields to updates and their new values.", required = true) @Valid @NotEmpty @RequestBody PatchServiceResourceRequest request) {
        try {
            String nodeType = request.getNodeInstance() == null ? null : request.getNodeInstance().getType();
            String nodeTypeVersion = request.getNodeInstance() == null ? null : request.getNodeInstance().getTypeVersion();
            Map<String, AbstractPropertyValue> nodeProperties = request.getNodeInstance() == null ? null : request.getNodeInstance().getProperties();
            Map<String, Capability> nodeCapabilities = request.getNodeInstance() == null ? null : request.getNodeInstance().getCapabilities();
            Map<String, String> nodeAttributeValues = request.getNodeInstance() == null ? null : request.getNodeInstance().getAttributeValues();

            serviceResourceService.patch(id, request.getName(), request.getVersion(), request.getDescription(), nodeType, nodeTypeVersion, nodeProperties,
                    nodeCapabilities, nodeAttributeValues, request.getLocationIds(), request.getCapabilitiesRelationshipTypes(),
                    request.getRequirementsRelationshipTypes());
            return RestResponseBuilder.<ConstraintInformation> builder().build();
        } catch (ConstraintViolationException | ConstraintValueDoNotMatchPropertyTypeException e) {
            if (e.getConstraintInformation() != null) {
                return RestConstraintValidator.fromException(e, e.getConstraintInformation().getName(), e.getConstraintInformation().getValue());
            } else {
                return RestConstraintValidator.fromException(e, null, null);
            }
        }
    }

    @ApiOperation(value = "Duplicate a service.", notes = "When the service is managed by alien (through application deployment) the only authorized patch are on location and authorizations.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/duplicate/{id}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ServiceResource> duplicate(
            @ApiParam(value = "Id of the service to duplicate.", required = true) @PathVariable @Valid @NotEmpty String id) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {

        String duplicateId = serviceResourceService.duplicate(id);
        ServiceResource serviceResource = serviceResourceService.getOrFail(duplicateId);
        return RestResponseBuilder.<ServiceResource> builder().data(serviceResource).build();
    }

    @ApiOperation(value = "Delete a service.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<Void> delete(@ApiParam(value = "Id of the service to delete.", required = true) @PathVariable @Valid @NotEmpty String id) {
        serviceResourceService.delete(id);
        return RestResponseBuilder.<Void> builder().build();
    }

    // .error(RestErrorBuilder.builder(RestErrorCode.DELETE_REFERENCED_OBJECT_ERROR).message(errorMessage).build())

    @ApiOperation(value = "Search services.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/adv/search", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<FacetedSearchResult> search(@RequestBody SortedSearchRequest searchRequest) {
        FacetedSearchResult result = serviceResourceService.search(searchRequest.getQuery(), searchRequest.getFilters(),
                searchRequest.getSortField(), searchRequest.isDesc(), searchRequest.getFrom(), searchRequest.getSize());
        return RestResponseBuilder.<GetMultipleDataResult<ServiceResource>> builder().data(result).build();
    }
}