package alien4cloud.orchestrators.rest;

import alien4cloud.orchestrators.services.OrchestratorFactoriesRegistry;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.Authorization;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Allow to query for orchestrator plugins.
 */
@RestController
@RequestMapping(value = "/rest/orchestrators", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Orchestrators", description = "Manages orchestrators.", authorizations = { @Authorization("ADMIN") })
public class OrchestratorPluginController {
    @Resource
    private OrchestratorFactoriesRegistry orchestratorFactoriesRegistry;


}