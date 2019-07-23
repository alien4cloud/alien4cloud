package alien4cloud.rest.wizard;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.images.IImageDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.rest.model.*;
import alien4cloud.rest.wizard.model.*;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.index.ArchiveIndexer;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

/**
 * Service that allows managing applications.
 */
@Slf4j
@RestController
@RequestMapping({"/rest/wizard", "/rest/v1/wizard", "/rest/latest/wizard"})
@Api(value = "", description = "Operations on Applications")
public class ApplicationWizardController {
    @Resource
    private IImageDAO imageDAO;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    ArchiveIndexer archiveIndexer;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationVersionService applicationVersionService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private TopologyService topologyService;
    @Resource
    private TopologyServiceCore topologyServiceCore;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    /**
     * Get an application from it's id.
     *
     * @param applicationId The application id.
     */
    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/applications/overview/{applicationId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<ApplicationOverview> get(@PathVariable String applicationId) {
        Application application = applicationService.checkAndGetApplication(applicationId);
        ApplicationOverview overview = new ApplicationOverview();
        // not usefull for the moment
        // applicationOverview.setApplication(application);

        overview.setNamedMetaProperties(getNamedMetaProperties(application.getMetaProperties()));

        ApplicationEnvironment applicationEnvironment = applicationEnvironmentService.getEnvironmentByIdOrDefault(applicationId, null);
        DeploymentStatus status = null;
        try {
            overview.setDeploymentStatus(applicationEnvironmentService.getStatus(applicationEnvironment));
        } catch (Exception e) {
            overview.setDeploymentStatus(DeploymentStatus.UNKNOWN);
        }

        overview.setDescription(application.getDescription());

        Topology topology = topologyServiceCore.getOrFail(applicationEnvironment.getApplicationId() + ":" + applicationEnvironment.getTopologyVersion());
        overview.setTopologyId(applicationEnvironment.getApplicationId());
        overview.setTopologyVersion(applicationEnvironment.getTopologyVersion());
        overview.setModules(getModules(topology));

        return RestResponseBuilder.<ApplicationOverview>builder().data(overview).build();
    }

    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/topologies/overview/{topologyId:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyOverview> getTopologyOverview(@PathVariable String topologyId) {
        TopologyOverview overview = new TopologyOverview();
        Topology topology = topologyServiceCore.getOrFail(topologyId);
        overview.setDescription(topology.getDescription());
        overview.setTopologyId(topology.getArchiveName());
        overview.setTopologyVersion(topology.getArchiveVersion());
        overview.setModules(getModules(topology));
        return RestResponseBuilder.<TopologyOverview>builder().data(overview).build();
    }

    private List<ApplicationModule> getModules(Topology topology) {
        List<ApplicationModule> modules = Lists.newArrayList();
        Map<String, NodeType> indexedNodeTypesFromTopology = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true, false);
        if (topology.getNodeTemplates() != null) {
            topology.getNodeTemplates().forEach((name, nodeTemplate) -> {
                NodeType nodeType = indexedNodeTypesFromTopology.get(name);
                ApplicationModule applicationModule = new ApplicationModule();
                applicationModule.setNodeType(nodeType);
                applicationModule.setNamedMetaProperties(getNamedMetaProperties(nodeType.getMetaProperties()));
                modules.add(applicationModule);
            });
        }
        return modules;
    }

    @ApiOperation(value = "Get an application based from its id.", notes = "Returns the application details. Application role required [ APPLICATION_MANAGER | APPLICATION_USER | APPLICATION_DEVOPS | DEPLOYMENT_MANAGER ]")
    @RequestMapping(value = "/topologies/graph/{topologyId:.+}:{topologyVersion:.+}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<TopologyGraph> getTopologyGraph(@PathVariable String topologyId, @PathVariable String topologyVersion) {
        Topology topology = topologyServiceCore.getOrFail(topologyId + ":" + topologyVersion);
        TopologyGraph topologyGraph = buildTopologyGraph(topology);
        return RestResponseBuilder.<TopologyGraph>builder().data(topologyGraph).build();
    }

    private List<MetaProperty> getNamedMetaProperties(Map<String, String> metaProperties) {
        List<MetaProperty> namedMetaProperties = Lists.newArrayList();
        metaProperties.forEach((id, value) -> {
            MetaPropConfiguration configuration = dao.findById(MetaPropConfiguration.class, id);
            // TODO: here filter returned meta properties for applications
            namedMetaProperties.add(new MetaProperty(configuration, value));
        });
        return namedMetaProperties;
    }

    private TopologyGraph buildTopologyGraph(Topology topology) {
        Map<String, NodeType> indexedNodeTypesFromTopology = topologyServiceCore.getIndexedNodeTypesFromTopology(topology, false, true, false);

        TopologyGraph topologyGraph = new TopologyGraph();
        List<TopologyGraphNode> nodes = Lists.newArrayList();
        List<TopologyGraphEdge> edges = Lists.newArrayList();
        topology.getNodeTemplates().forEach((nodeName, nodeTemplate) -> {
            TopologyGraphNode node = new TopologyGraphNode();
            node.setId(nodeName);
            node.setLabel(nodeName);
            node.setNodeType(indexedNodeTypesFromTopology.get(nodeName));
            nodes.add(node);
            if (nodeTemplate.getRelationships() != null) {
                nodeTemplate.getRelationships().forEach((id, relationshipTemplate) -> {
                    TopologyGraphEdge edge = new TopologyGraphEdge();
                    edge.setId(id);
                    edge.setSource(nodeName);
                    edge.setTarget(relationshipTemplate.getTarget());
                    String words[] = relationshipTemplate.getType().split(".");
                    edge.setLabel((relationshipTemplate.getType().lastIndexOf(".") > -1) ? relationshipTemplate.getType().substring(relationshipTemplate.getType().lastIndexOf(".") + 1) : relationshipTemplate.getType());
                    edges.add(edge);
                });
            }
        });
        topologyGraph.setNodes(nodes);
        topologyGraph.setEdges(edges);
        return topologyGraph;
    }

}
