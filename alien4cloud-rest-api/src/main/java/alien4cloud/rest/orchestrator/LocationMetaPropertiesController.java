package alien4cloud.rest.orchestrator;

import javax.annotation.Resource;

import alien4cloud.model.common.IMetaProperties;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
import alien4cloud.rest.common.AbstractMetaPropertyController;
import alien4cloud.rest.internal.model.PropertyRequest;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.audit.annotation.Audit;
import alien4cloud.common.MetaPropertiesService;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import org.alien4cloud.tosca.exceptions.ConstraintValueDoNotMatchPropertyTypeException;
import org.alien4cloud.tosca.exceptions.ConstraintViolationException;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;

/**
 * Allow to manage the orchestrator properties
 */
@Slf4j
@RestController
@RequestMapping(value = { "/rest/orchestrators/{orchestratorId}/locations/{locationId}/properties",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/properties",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/properties" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Location meta properties", description = "Update values for meta-properties associated with locations.", authorizations = {
        @Authorization("ADMIN") })
public class LocationMetaPropertiesController extends AbstractMetaPropertyController {
    @Resource
    private LocationService locationService;

    /**
     * Update or create a meta-property for a location.
     *
     * @param orchestratorId id of the orchestrator the location belongs to.
     * @param locationId id of the location to update
     * @param propertyRequest property request
     * @return information on the constraint
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintInformation> upsertMetaProperty(
            @ApiParam(value = "Id of the orchestrator for which the location is defined.") @PathVariable String orchestratorId,
            @ApiParam(value = "Id of the location to get", required = true) @PathVariable String locationId,
            @ApiParam(value = "Id of the location to get", required = true) @RequestBody PropertyRequest propertyRequest)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        return super.upsertProperty(locationId, propertyRequest);
    }

    @Override
    protected IMetaProperties getTarget(String locationId) {
        return locationService.getOrFail(locationId);
    }
}
