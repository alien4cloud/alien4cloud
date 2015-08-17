package alien4cloud.rest.orchestrator;

import javax.annotation.Resource;

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
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.rest.internal.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.security.model.Role;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Allow to manage the orchestrator properties
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/orchestrators/{id}/properties", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators", description = "Manages orchestrators.", authorizations = { @Authorization("ADMIN") })
public class OrchestratorPropertiesController {
    @Resource
    private OrchestratorService orchestratorService;
    @Resource
    private MetaPropertiesService metaPropertiesService;

    /**
     * Update or create a property for an cloud
     *
     * @param id id of the orchestrator to update
     * @param propertyRequest property request
     * @return information on the constraint
     */
    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ADMIN')")
    @Audit
    public RestResponse<ConstraintInformation> upsertMetaProperty(@PathVariable String id, @RequestBody PropertyRequest propertyRequest)
            throws ConstraintViolationException, ConstraintValueDoNotMatchPropertyTypeException {
        AuthorizationUtil.hasOneRoleIn(Role.ADMIN);
        Orchestrator orchestrator = orchestratorService.getOrFail(id);

        try {
            metaPropertiesService.upsertMetaProperty(orchestrator, propertyRequest.getDefinitionId(), propertyRequest.getValue());
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