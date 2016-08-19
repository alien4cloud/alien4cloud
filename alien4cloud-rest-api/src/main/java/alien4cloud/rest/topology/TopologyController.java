package alien4cloud.rest.topology;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.TopologyDTOBuilder;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.application.ApplicationVersionService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.AbstractTopologyVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.model.ApplicationRole;
import alien4cloud.topology.*;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping({ "/rest/topologies", "/rest/v1/topologies", "/rest/latest/topologies" })
public class TopologyController {

    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource
    private TopologyValidationService topologyValidationService;

    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private TopologyTemplateVersionService topologyTemplateVersionService;

    @Resource
    private EditionContextManager topologyEditionContextManager;
    @Inject
    private TopologyDTOBuilder dtoBuilder;

    /**
     * Retrieve an existing {@link alien4cloud.model.topology.Topology}
     *
     * @param topologyId The id of the topology to retrieve.
     * @return {@link RestResponse}<{@link TopologyDTO}> containing the {@link alien4cloud.model.topology.Topology} and the {@link IndexedNodeType} related
     *         to his {@link alien4cloud.model.topology.NodeTemplate}s
     */
    @ApiOperation(value = "Retrieve a topology from it's id.", notes = "Returns a topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyDTO> get(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS,
                ApplicationRole.APPLICATION_USER);
        try {
            topologyEditionContextManager.init(topologyId);
            return RestResponseBuilder.<TopologyDTO> builder().data(dtoBuilder.buildTopologyDTO(EditionContextManager.get())).build();
        } finally {
            topologyEditionContextManager.destroy();
        }
    }

    /**
     * check if a topology is valid or not.
     *
     * @param topologyId The id of the topology to check.
     * @return a boolean rest response that says if the topology is valid or not.
     */
    @ApiOperation(value = "Check if a topology is valid or not.", notes = "Returns true if valid, false if not. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/isvalid", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyValidationResult> isTopologyValid(@PathVariable String topologyId, @RequestParam(required = false) String environmentId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS,
                ApplicationRole.APPLICATION_USER);
        TopologyValidationResult dto = topologyValidationService.validateTopology(topology);
        return RestResponseBuilder.<TopologyValidationResult> builder().data(dto).build();
    }

    /**
     * Retrieve an existing {@link alien4cloud.model.topology.Topology} as YAML
     *
     * @param topologyId The id of the topology to retrieve.
     * @return {@link RestResponse}<{@link String}> containing the topology as YAML
     */
    @RequestMapping(value = "/{topologyId}/yaml", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<String> getYaml(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        topologyService.checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS,
                ApplicationRole.APPLICATION_USER);
        String yaml = topologyService.getYaml(topology);
        return RestResponseBuilder.<String> builder().data(yaml).build();
    }

    @ApiOperation(value = "Get the version of application or topology template related to this topology.")
    @RequestMapping(value = "/{topologyId:.+}/version", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<AbstractTopologyVersion> getVersion(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        if (topology == null) {
            throw new NotFoundException("No topology found for " + topologyId);
        }
        AbstractTopologyVersion version = null;
        if (topology.getDelegateType().equalsIgnoreCase(TopologyTemplate.class.getSimpleName())) {
            version = topologyTemplateVersionService.getByTopologyId(topologyId);
        } else {
            version = applicationVersionService.getByTopologyId(topologyId);
        }
        if (version == null) {
            throw new NotFoundException("No version found for topology " + topologyId);
        }
        return RestResponseBuilder.<AbstractTopologyVersion> builder().data(version).build();
    }
}
