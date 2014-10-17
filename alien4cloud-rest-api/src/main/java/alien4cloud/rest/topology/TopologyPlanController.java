package alien4cloud.rest.topology;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import alien4cloud.paas.plan.StartEvent;
import alien4cloud.rest.model.RestResponse;

/**
 * Get plans for a topology.
 */
@RequestMapping("/topologies/plan")
public class TopologyPlanController {

    @RequestMapping(value = "/{topologyId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<StartEvent> getCreationPlan(String topologyId) {

        return new RestResponse<StartEvent>();
        // StartEvent startEvent = PaaSPlanGenerator.buildNodeStartPlan(nodeTemplate);
    }
}
