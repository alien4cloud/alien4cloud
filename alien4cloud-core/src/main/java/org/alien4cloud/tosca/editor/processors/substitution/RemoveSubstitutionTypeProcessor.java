package org.alien4cloud.tosca.editor.processors.substitution;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.TopologyService;

/**
 * Delete the substitute of a topology template.
 */
@Component
public class RemoveSubstitutionTypeProcessor implements IEditorCommitableProcessor<RemoveSubstitutionTypeOperation> {
    @Resource
    private TopologyService topologyService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Override
    public void process(Csar csar, Topology topology, RemoveSubstitutionTypeOperation operation) {

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        // FIXME check also on live edited topologies.

        // the substitute type os within the topology's archive
        if (hasArchiveUsing(csar.getName(), csar.getVersion())) {
            throw new DeleteReferencedObjectException("The substitution can not be removed since it's type is already used in at least another topology");
        }

        topologyService.unloadType(topology, topology.getSubstitutionMapping().getSubstitutionType());
        topology.setSubstitutionMapping(null);
    }

    @Override
    public void beforeCommit(RemoveSubstitutionTypeOperation operation) {
        // here we should eventually remove from the repo the type related to this substitution
        alienDAO.delete(NodeType.class, EditionContextManager.getTopology().getId());
    }

    private boolean hasArchiveUsing(String archiveName, String archiveVersion) {
        // FilterBuilders.boolFilter().mustNot(FilterBuilders.boolFilter().must()
        // .must());
        FilterBuilder notThisArchiveFilter = FilterBuilders
                .notFilter(FilterBuilders.andFilter(FilterBuilders.termFilter("name", archiveName), FilterBuilders.termFilter("version", archiveVersion)));

        return alienDAO.buildQuery(Csar.class)
                .setFilters(fromKeyValueCouples("dependencies.name", archiveName, "dependencies.version", archiveVersion), notThisArchiveFilter).count() > 0;
    }
}