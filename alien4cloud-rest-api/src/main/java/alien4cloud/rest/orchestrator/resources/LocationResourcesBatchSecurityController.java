package alien4cloud.rest.orchestrator.resources;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.rest.orchestrator.AbstractLocationResourcesBatchSecurityController;
import io.swagger.annotations.Api;

@RestController
@RequestMapping({ "/rest/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/",
        "/rest/v1/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/",
        "/rest/latest/orchestrators/{orchestratorId}/locations/{locationId}/resources/security/" })
@Api(value = "", description = "Location resource security batch operations")
public class LocationResourcesBatchSecurityController extends AbstractLocationResourcesBatchSecurityController {

    // Nothing here for now, this controller is just to bind another entry endpoint to common resources batch security controller
}
