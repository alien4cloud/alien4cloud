package alien4cloud.rest.orchestrator;

import javax.annotation.Resource;

import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.locations.services.LocationService;
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
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;

import java.util.Date;

/**
 * Allow to manage the orchestrator properties
 */
@Slf4j
@RestController
@RequestMapping(value = {"/rest/orchestrators/{orchestratorId}/locations/{locationId}/properties", "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/properties", "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/properties", "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/properties", "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/properties"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Location meta properties", description = "Update values for meta-properties associated with locations.", authorizations = {
        @Authorization("ADMIN") })
public class LocationMetaPropertiesController {
    @Resource
    private LocationService locationService;
    @Resource
    private MetaPropertiesService metaPropertiesService;

    /**
     * Update or create a property for an orchestrator
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
        Location location = locationService.getOrFail(locationId);

        try {
            metaPropertiesService.upsertMetaProperty(location, propertyRequest.getDefinitionId(), propertyRequest.getValue());
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation error for property <" + propertyRequest.getDefinitionId() + "> with value <" + propertyRequest.getValue() + ">", e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        }
        return RestResponseBuilder.<ConstraintInformation> builder().data(null).error(null).build();
    }
}
