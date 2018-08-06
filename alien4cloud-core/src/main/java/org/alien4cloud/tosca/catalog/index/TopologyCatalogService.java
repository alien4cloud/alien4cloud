package org.alien4cloud.tosca.catalog.index;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.dao.model.FetchContext.SUMMARY;

import java.util.Map;

import javax.inject.Inject;

import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.tosca.parser.ToscaParser;
import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.NameValidationUtils;
import alien4cloud.utils.VersionUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for indexing and updating topologies.
 */
@Slf4j
@Service
public class TopologyCatalogService extends AbstractToscaIndexSearchService<Topology> implements ITopologyCatalogService {
    @Inject
    private ArchiveIndexer archiveIndexer;
    @Inject
    private WorkflowsBuilderService workflowBuilderService;

    @Override
    public Topology createTopologyAsTemplate(String name, String description, String version, String workspace, String fromTopologyId) {
        NameValidationUtils.validate("topologyTemplateName", name);
        // Every version of a topology template has a Cloud Service Archive
        Csar csar = new Csar(name, StringUtils.isNotBlank(version) ? version : VersionUtil.DEFAULT_VERSION_NAME);
        csar.setWorkspace(workspace);
        csar.setDelegateType(ArchiveDelegateType.CATALOG.toString());
        csar.setToscaDefinitionsVersion(ToscaParser.LATEST_DSL);
        if (description == null) {
            csar.setDescription("This archive has been created with alien4cloud.");
        } else {
            csar.setDescription("Enclosing archive for topology " + description);
        }

        Topology topology;
        if (fromTopologyId != null) { // "cloning" the topology
        	// TODO Currently, the fromTopologyId is always null. If this implementation needed, please think about initializing the workflow
            topology = alienDAO.findById(Topology.class, fromTopologyId);
        } else {
            topology = new Topology();
            // Init the workflow if the topology is totally new
            workflowBuilderService.initWorkflows(workflowBuilderService.buildTopologyContext(topology, csar));
        }
        topology.setDescription(description);
        topology.setArchiveName(csar.getName());
        topology.setArchiveVersion(csar.getVersion());
        topology.setWorkspace(csar.getWorkspace());
        archiveIndexer.importNewArchive(csar, topology, null);
        return topology;
    }

    @Override
    protected Topology[] getArray(int size) {
        return new Topology[size];
    }

    @Override
    protected String getAggregationField() {
        return "archiveName";
    }

    // we need to override for aspect purpose
    @Override
    public FacetedSearchResult search(Class<? extends Topology> clazz, String query, Integer size, Map<String, String[]> filters) {
        return super.search(clazz, query, size, filters);
    }

    @Override
    public Topology[] getAll(Map<String, String[]> filters, String archiveName) {
        return alienDAO.buildQuery(Topology.class)
                .setFilters(fromKeyValueCouples(filters, "workspace", AlienConstants.GLOBAL_WORKSPACE_ID, "archiveName", archiveName)).prepareSearch()
                .setFetchContext(SUMMARY).search(0, Integer.MAX_VALUE).getData();
    }

    @Override
    public Topology getOrFail(String id) {
        Topology topology = get(id);
        if (topology == null) {
            throw new NotFoundException("Unable to find a topology with id <" + id + ">");
        }
        return topology;
    }

    @Override
    public Topology get(String id) {
        return alienDAO.findById(Topology.class, id);
    }

    /**
     * Return true if the given id exists.
     *
     * @param id The id to check.
     * @return True if a topology with the given id exists, false if not.
     */
    @Override
    public boolean exists(String id) {
        return alienDAO.exist(Topology.class, id);
    }
}
