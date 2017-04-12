package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorCommitableProcessor;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.NodeType;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.springframework.stereotype.Component;

import alien4cloud.dao.FilterUtil;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FetchContext;
import alien4cloud.dao.model.GetMultipleDataResult;
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
    public void process(RemoveSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();

        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }

        // FIXME also check if no element inherits from this substitute type
        // the substitute type os within the topology's archive
        Csar csar = EditionContextManager.getCsar();
        Topology[] topologies = getTopologiesUsing(csar.getName(), csar.getName(), csar.getVersion());
        if (ArrayUtils.isNotEmpty(topologies)) {
            throw new DeleteReferencedObjectException("The substitution can not be removed since it's type is already used in at least another topology");
        }

        topologyService.unloadType(topology, new String[] { topology.getSubstitutionMapping().getSubstitutionType() });
        topology.setSubstitutionMapping(null);
    }

    @Override
    public void beforeCommit(RemoveSubstitutionTypeOperation operation) {
        // here we should eventually remove from the repo the type related to this substitution
        alienDAO.delete(NodeType.class, EditionContextManager.getTopology().getId());
    }

    private Topology[] getTopologiesUsing(String elementId, String archiveName, String archiveVersion) {
        FilterBuilder customFilter = FilterBuilders.boolFilter()
                .mustNot(FilterBuilders.boolFilter().must(FilterBuilders.termFilter("archiveName", archiveName))
                        .must(FilterBuilders.termFilter("archiveVersion", archiveVersion)))
                .must(FilterBuilders.nestedFilter("dependencies", FilterBuilders.boolFilter().must(FilterBuilders.termFilter("dependencies.name", archiveName))
                        .must(FilterBuilders.termFilter("dependencies.version", archiveVersion))));
        Map<String, String[]> filter = FilterUtil.singleKeyFilter("nodeTemplates.value.type", elementId);
        GetMultipleDataResult<Topology> result = alienDAO.search(Topology.class, null, filter, customFilter, FetchContext.SUMMARY, 0, Integer.MAX_VALUE);
        return result.getData();
    }

}