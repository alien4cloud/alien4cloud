package alien4cloud.rest.health;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.events.HALeaderElectionEvent;

/**
 * Service that allows managing applications.
 */
@Slf4j
@RestController
@RequestMapping({ "/rest/v1/health", "/rest/latest/health" })
@Api(value = "", description = "Health check endpoint")
public class HealthController {

    @Resource
    private ApplicationContext alienContext;

    @ApiOperation(value = "Healthcheck endpoint")
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    public String check() {
        if (log.isTraceEnabled()) {
            log.trace("Health checked");
        }
        // TODO: here we should try to contact ES, try to write something on FS ?
        // TODO: To avoid DoS we should ensure to enqueue requests (do not accept every check request in parallel)
        return "OK";
    }

    @RequestMapping(value = "/elect", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @Deprecated
    // TODO: remove when consul algo will be implemented
    public void elect() {
        log.info("Instance is elected as leader");
        alienContext.publishEvent(new HALeaderElectionEvent(this, true));
    }

    @RequestMapping(value = "/banish", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.OK)
    @Deprecated
    // TODO: remove when consul algo will be implemented
    public void banish() {
        log.info("Instance is banish (no more leader)");
        alienContext.publishEvent(new HALeaderElectionEvent(this, false));
    }

}
