package alien4cloud.rest.orchestrator;

import java.util.Map;

import javax.inject.Inject;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import alien4cloud.orchestrators.services.OrchestratorDeploymentService;
import alien4cloud.rest.internal.model.PropertyRequest;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.tosca.properties.constraints.ConstraintUtil;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import springfox.documentation.annotations.ApiIgnore;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 *
 */
@Slf4j
@ApiIgnore
@RestController
@RequestMapping(value = {"/rest/orchestrators/{orchestratorId}", "/rest/v1/orchestrators/{orchestratorId}", "/rest/latest/orchestrators/{orchestratorId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
public class OrchestratorDeploymentPropertiesController {
    @Inject
    private OrchestratorDeploymentService orchestratorDeploymentService;
    @Inject
    private ConstraintPropertyService constraintPropertyService;

    @ApiIgnore
    @RequestMapping(value = "/deployment-prop-check", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ConstraintUtil.ConstraintInformation> checkPluginDeploymentProperties(
            @ApiParam(value = "Id of the orchestrators for which to check deployment property.", required = true) @PathVariable @Valid @NotEmpty String orchestratorId,
            @ApiParam(value = "Value and id of the property to check.", required = true) @Valid @NotEmpty @RequestBody PropertyRequest deploymentPropertyValidationRequest) {
        Map<String, PropertyDefinition> deploymentPropertyDefinitions = orchestratorDeploymentService.getDeploymentPropertyDefinitions(orchestratorId);

        if (deploymentPropertyDefinitions != null) {
            PropertyDefinition propertyDefinition = deploymentPropertyDefinitions.get(deploymentPropertyValidationRequest.getDefinitionId());
            if (propertyDefinition != null && propertyDefinition.getConstraints() != null) {
                try {
                    constraintPropertyService.checkSimplePropertyConstraint(deploymentPropertyValidationRequest.getDefinitionId(),
                            deploymentPropertyValidationRequest.getValue(), propertyDefinition);
                } catch (ConstraintViolationException e) {
                    log.error("Constraint violation error for property <" + deploymentPropertyValidationRequest.getDefinitionId() + "> with value <"
                            + deploymentPropertyValidationRequest.getValue() + ">", e);
                    return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                            .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
                } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                    log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                            + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
                    return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().data(e.getConstraintInformation())
                            .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
                }
            }
        }
        return RestResponseBuilder.<ConstraintUtil.ConstraintInformation> builder().build();
    }

    /**
     * Get deployment properties for an orchestrator.
     *
     * @param orchestratorId Id of the orchestrator for which to get properties.
     */
    @ApiOperation(value = "Get deployment properties for an orchestrator.", notes = "Deployments properties are properties that can be set by the Application Deployer before deployment. They depends on the IPaaSProvider plugin associated with an orchestrator.", authorizations = {
            @Authorization("ADMIN") })
    @RequestMapping(value = "/deployment-property-definitions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Map<String, PropertyDefinition>> getDeploymentPropertyDefinitions(
            @ApiParam(value = "Id of the cloud for which to get details.", required = true) @Valid @NotBlank @PathVariable String orchestratorId) {
        return RestResponseBuilder.<Map<String, PropertyDefinition>> builder()
                .data(orchestratorDeploymentService.getDeploymentPropertyDefinitions(orchestratorId)).build();
    }
}
