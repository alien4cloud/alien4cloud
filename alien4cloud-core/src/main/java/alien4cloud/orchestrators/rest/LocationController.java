package alien4cloud.orchestrators.rest;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.Authorization;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that manages locations for orchestrators.
 */
@RestController
@RequestMapping(value = "/rest/orchestrators/{id}/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrator's Locations", description = "Manages locations for a given orchestrator.", authorizations = { @Authorization("ADMIN") }, position = 4400)
public class LocationController {
}
