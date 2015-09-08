package alien4cloud.rest.orchestrator;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.rest.model.RestResponse;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;

/**
 * Deployment properties are specific option provided by an orchestrators plugin and that are added to the deployment parameters in addition to thoses related to
 * the topology or matched nodes.
 * This allows to take in account some specific features from orchestrators.
 */

public class OrchestratorDeploymentProperties {
    @ApiOperation(value = "Get deployment properties for a cloud.", notes = "Deployments properties are properties that can be set by the Application Deployer before deployment. They depends on the IPaaSProvider plugin associated with a cloud.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{id}/deploymentpropertydefinitions", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Map<String, PropertyDefinition>> get() {
        return null;
    }
}
