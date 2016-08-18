package alien4cloud.rest.topology;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyRecoveryService;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping({ "/rest/topologies", "/rest/v1/topologies", "/rest/latest/topologies" })
public class TopologyRecoveryController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Inject
    private TopologyServiceCore topologyServiceCore;

    @Inject
    private TopologyService topologyService;

    @Inject
    private TopologyRecoveryService topologySynchService;

    /**
     * Retrieve csar dependencies of a given topology have been updated since they were added into the topology.
     *
     * @param topologyId The id of the topology to check.
     * @return {@link RestResponse}<{@link Set}<{@link CSARDependency}>> containing the dependencies that have been updated.
     * 
     */
    @ApiOperation(value = "Retrieve csar dependencies of a given topology have been updated since they were added into the topology.", notes = "Returns a set of dependencies that have been updated. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/updatedDependencies", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Set<CSARDependency>> getUpdatedDependencies(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS,
                ApplicationRole.APPLICATION_USER);
        return RestResponseBuilder.<Set<CSARDependency>> builder().data(topologySynchService.getUpdatedDependencies(topology)).build();
    }

    /**
     * Recover the topology, synchronize with the existing elements in the database of a set of csar dependencies
     *
     * @param topologyId The id of the topology to synch.
     * @return {@link RestResponse}<{@link Set}<{@link CSARDependency}>> containing the dependencies that have been updated.
     * 
     */
    @ApiOperation(value = "Recover the topology, synch it with the existing element in the database of a set of csar dependencies.", notes = "Returns a synchronized topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/recover", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyDTO> recoverTopology(@PathVariable String topologyId, @RequestBody Set<CSARDependency> dependenciesToSynch) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS,
                ApplicationRole.APPLICATION_USER);
        topologyService.throwsErrorIfReleased(topology);
        topologySynchService.recoverTopology(dependenciesToSynch, topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

}
