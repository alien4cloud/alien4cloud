package alien4cloud.rest.runtime;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.audit.annotation.Audit;
import alien4cloud.cloud.DeploymentService;
import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.components.IOperationParameter;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.rest.model.RestError;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.rest.topology.TopologyDTO;
import alien4cloud.rest.topology.TopologyService;
import alien4cloud.security.ApplicationEnvironmentRole;
import alien4cloud.security.ApplicationRole;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintFunctionalException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintRequiredParameterException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.Authorization;

@RestController
@Slf4j
@RequestMapping("/rest/runtime")
public class RuntimeController {

    @Resource
    private DeploymentService deploymentService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Resource
    private CSARRepositorySearchService csarRepoSearchService;

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @ApiOperation(value = "Trigger a custom command on a specific node template of a topology .", authorizations = { @Authorization("APPLICATION_MANAGER") }, notes = "Returns a response with no errors and the command response as data in success case. Application role required [ APPLICATION_MANAGER ]")
    @RequestMapping(value = "/{applicationId:.+?}/operations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @Audit
    public DeferredResult<RestResponse<Object>> executeOperation(@PathVariable String applicationId, @RequestBody @Valid OperationExecRequest operationRequest) {
        final DeferredResult<RestResponse<Object>> result = new DeferredResult<>();
        Application application = applicationService.getOrFail(applicationId);
        AuthorizationUtil.checkAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER);

        // validate the operation request
        try {
            validateCommand(operationRequest);
        } catch (ConstraintViolationException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder().data(e.getConstraintInformation())
                    .error(new RestError(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR.getCode(), e.getMessage())).build());
            return result;
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder().data(e.getConstraintInformation())
                    .error(new RestError(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR.getCode(), e.getMessage())).build());
            return result;
        } catch (ConstraintRequiredParameterException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder().data(e.getConstraintInformation())
                    .error(new RestError(RestErrorCode.PROPERTY_REQUIRED_VIOLATION_ERROR.getCode(), e.getMessage())).build());
            return result;
        } catch (ConstraintFunctionalException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder().data(e.getConstraintInformation())
                    .error(new RestError(RestErrorCode.PROPERTY_UNKNOWN_VIOLATION_ERROR.getCode(), e.getMessage())).build());
            return result;
        }

        // try to trigger the execution of the operation
        try {
            deploymentService.triggerOperationExecution(operationRequest, new IPaaSCallback<Map<String, String>>() {
                @Override
                public void onSuccess(Map<String, String> data) {
                    result.setResult(RestResponseBuilder.<Object> builder().data(data).build());
                }

                @Override
                public void onFailure(Throwable throwable) {
                    result.setErrorResult(RestResponseBuilder.<Object> builder()
                            .error(new RestError(RestErrorCode.NODE_OPERATION_EXECUTION_ERROR.getCode(), throwable.getMessage())).build());
                }
            });
        } catch (OperationExecutionException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder()
                    .error(new RestError(RestErrorCode.NODE_OPERATION_EXECUTION_ERROR.getCode(), e.getMessage())).build());
        } catch (CloudDisabledException e) {
            result.setErrorResult(RestResponseBuilder.<Object> builder().error(new RestError(RestErrorCode.CLOUD_DISABLED_ERROR.getCode(), e.getMessage()))
                    .build());
        }
        return result;
    }

    /**
     * Get runtime (deployed) topology of an application on a specific environment
     * 
     * @param applicationId application id for which to get the topology
     * @param applicationEnvironmentId application environment for which to get the topology through the version
     * @return {@link RestResponse}<{@link TopologyDTO}> containing the requested runtime {@link Topology} and the
     *         {@link alien4cloud.model.components.IndexedNodeType} related to his {@link NodeTemplate}s
     */
    @ApiOperation(value = "Get runtime (deployed) topology of an application on a specific cloud.")
    @RequestMapping(value = "/{applicationId:.+?}/environment/{applicationEnvironmentId:.+?}/topology", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> getDeployedTopology(
            @ApiParam(value = "Id of the application for which to get deployed topology.", required = true) @PathVariable String applicationId,
            @ApiParam(value = "Id of the environment for which to get deployed topology.", required = true) @PathVariable String applicationEnvironmentId) {

        // get the topology linked to the current environment
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(applicationEnvironmentId);
        Application application = applicationService.checkAndGetApplication(applicationId);
        if (!AuthorizationUtil.hasAuthorizationForApplication(application, ApplicationRole.APPLICATION_MANAGER)) {
            AuthorizationUtil.checkAuthorizationForEnvironment(applicationEnvironment, ApplicationEnvironmentRole.DEPLOYMENT_MANAGER);
        }

        String topologyId = applicationEnvironmentService.getTopologyId(applicationEnvironmentId);
        String cloudId = applicationEnvironment.getCloudId();

        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(deploymentService.getRuntimeTopology(topologyId, cloudId)))
                .build();
    }

    private void validateCommand(OperationExecRequest operationRequest) throws ConstraintFunctionalException {

        // get the targeted environment
        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getOrFail(operationRequest.getApplicationEnvironmentId());
        String cloudId = applicationEnvironment.getCloudId();
        String topologyId = applicationEnvironmentService.getTopologyId(operationRequest.getApplicationEnvironmentId());

        // get if exists the runtime version of the topology
        Topology topology = deploymentService.getRuntimeTopology(topologyId, cloudId);

        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, operationRequest.getNodeTemplateName(),
                topologyServiceCore.getNodeTemplates(topology));
        IndexedNodeType indexedNodeType = csarRepoSearchService.getRequiredElementInDependencies(IndexedNodeType.class, nodeTemplate.getType(),
                topology.getDependencies());
        Map<String, Interface> interfaces = indexedNodeType.getInterfaces();

        if (interfaces == null || interfaces.get(operationRequest.getInterfaceName()) == null) {
            throw new NotFoundException("Interface [" + operationRequest.getInterfaceName() + "] not found in the node template ["
                    + operationRequest.getNodeTemplateName() + "] related to [" + indexedNodeType.getId() + "]");
        }

        Interface interfass = interfaces.get(operationRequest.getInterfaceName());

        validateOperation(interfass, operationRequest);
    }

    private void validateParameters(Interface interfass, OperationExecRequest operationRequest) throws ConstraintViolationException,
            ConstraintValueDoNotMatchPropertyTypeException, ConstraintRequiredParameterException {
        ArrayList<String> missingParams = Lists.newArrayList();

        Operation operation = interfass.getOperations().get(operationRequest.getOperationName());

        if (operation.getInputParameters() != null) {
            for (Entry<String, IOperationParameter> inputParameter : operation.getInputParameters().entrySet()) {
                if (inputParameter.getValue().isDefinition()) {
                    String requestInputParameter = operationRequest.getParameters() == null ? null : operationRequest.getParameters().get(
                            inputParameter.getKey());
                    PropertyDefinition currentOperationParameter = (PropertyDefinition) inputParameter.getValue();
                    if (StringUtils.isNotBlank(requestInputParameter)) {
                        // recover the good property definition for the current parameter
                        constraintPropertyService.checkPropertyConstraint(inputParameter.getKey(), requestInputParameter, currentOperationParameter);
                    } else if (currentOperationParameter.isRequired()) {
                        // input param not in the request, id required this is a missing parameter...
                        missingParams.add(inputParameter.getKey());
                    } else {
                        // set the value to null
                        operation.getInputParameters().put(inputParameter.getKey(), null);
                    }
                }
            }
        }

        // check required input issue
        if (!missingParams.isEmpty()) {
            log.error("Missing required parameter", missingParams);
            ConstraintInformation constraintInformation = new ConstraintInformation(null, null, missingParams.toString(), "required");
            throw new ConstraintRequiredParameterException("Missing required parameters", null, constraintInformation);
        }
    }

    private void validateOperation(Interface interfass, OperationExecRequest operationRequest) throws ConstraintFunctionalException {
        Operation operation = interfass.getOperations().get(operationRequest.getOperationName());
        if (operation == null) {
            throw new NotFoundException("Operation [" + operationRequest.getOperationName() + "] is not defined in the interface ["
                    + operationRequest.getInterfaceName() + "] of the node [" + operationRequest.getNodeTemplateName() + "]");
        }

        // validate parameters (value/type and required value)
        validateParameters(interfass, operationRequest);
    }
}
