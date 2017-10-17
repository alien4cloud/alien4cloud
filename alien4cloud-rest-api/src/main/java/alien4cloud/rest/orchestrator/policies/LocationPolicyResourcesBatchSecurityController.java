package alien4cloud.rest.orchestrator.policies;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.orchestrator.AbstractLocationResourcesBatchSecurityController;
import io.swagger.annotations.Api;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/policies/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/policies/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/policies/security/" })
@Api(value = "", description = "Location policy resource security batch operations")
public class LocationPolicyResourcesBatchSecurityController extends AbstractLocationResourcesBatchSecurityController {

    // Nothing here for now, this controller is just to bind another entry endpoint to common resources batch security controller
}
