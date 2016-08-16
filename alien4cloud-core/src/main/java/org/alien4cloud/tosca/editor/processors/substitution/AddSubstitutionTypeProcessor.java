package org.alien4cloud.tosca.editor.processors.substitution;

import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.AddSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.topology.SubstitutionMapping;
import alien4cloud.model.topology.Topology;

/**
 * Process the creation of topology template as substitute.
 */
@Component
public class AddSubstitutionTypeProcessor implements IEditorOperationProcessor<AddSubstitutionTypeOperation> {

    @Resource
    private CSARRepositorySearchService csarRepoSearchService;


    @Override
    public void process(AddSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        if (!topology.getDelegateType().equals(TopologyTemplate.class.getSimpleName().toLowerCase())) {
            throw new InvalidArgumentException("This operation is only allowed for topology templates");
        }

        if (topology.getSubstitutionMapping() == null) {
            topology.setSubstitutionMapping(new SubstitutionMapping());
        }

        IndexedNodeType nodeType = csarRepoSearchService.getElementInDependencies(IndexedNodeType.class, operation.getElementId(), topology.getDependencies());
        // if not null the node type exists in the dependencies, there is no choices for this type version
        if (nodeType == null) {
            // the node type does'nt exist in this topology dependencies
            // we need to find the latest version of this component and use it as default
            Map<String, String[]> filters = Maps.newHashMap();
            filters.put("elementId", new String[] { operation.getElementId() });
            FacetedSearchResult result = csarRepoSearchService.search(IndexedNodeType.class, null, 0, 1, filters, false);
            if (result.getTotalResults() > 0) {
                nodeType = (IndexedNodeType) result.getData()[0];
            }
            // add in dependencies
            topology.getDependencies().add(new CSARDependency(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
        }
        topology.getSubstitutionMapping().setSubstitutionType(nodeType);
    }
}