package alien4cloud.rest.orchestrator.policies;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.orchestrator.AbstractLocationResourcesSecurityController;
import io.swagger.annotations.Api;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/policies/{resourceId}/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/policies/{resourceId}/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/policies/{resourceId}/security/" })
@Api(value = "", description = "Location policy resources security operations")
public class LocationPolicyResourcesSecurityController extends AbstractLocationResourcesSecurityController {

    // Nothing here for now, this controller is just to bind another entry endpoint to common resources security controller

}
