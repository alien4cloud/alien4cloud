package alien4cloud.rest.topology;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.topology.Topology;
import alien4cloud.model.topology.TopologyLayout;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import io.swagger.annotations.Api;
import org.springframework.data.geo.Point;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Manages layout updates for a given topology.
 */
@RestController
@RequestMapping("/rest/topology/{topologyId}/layout")
@Api(value = "", description = "Operations on Applications")
public class TopologyLayoutController {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyService topologyService;

    /**
     * Get the layout for a given topology.
     * 
     * @param topologyId The id of the topology for which to get layout.
     * @return A rest response that contains the topology layout.
     */
    public RestResponse<TopologyLayout> get(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService
                .checkAuthorizations(topology, ApplicationRole.APPLICATION_USER, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_MANAGER);
        TopologyLayout layout = alienDAO.findById(TopologyLayout.class, topologyId);
        return RestResponseBuilder.<TopologyLayout> builder().data(layout).build();
    }

    /**
     * Set the new location of a root node in the layout.
     *
     * @param topologyId The id of the topology for which to edit a node placement.
     * @param nodeName The name of the node to edit.
     * @param point The new location for the node.
     * @return A void RestResponse.
     */
    @RequestMapping(value = "/{nodeName}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<Void> setNodeLocation(@PathVariable String topologyId, @PathVariable String nodeName, @RequestBody Point point) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        TopologyLayout layout = alienDAO.findById(TopologyLayout.class, topologyId);
        layout.getNodeLocations().put(nodeName, point);
        return RestResponseBuilder.<Void> builder().build();
    }
}