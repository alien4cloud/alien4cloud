package org.alien4cloud.tosca.editor.processors.substitution;

import alien4cloud.exception.NotFoundException;
import alien4cloud.topology.TopologyService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.SetSubstitutionCapabilityServiceRelationshipOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

/**
 * Created by lucboutier on 12/04/2017.
 */
public abstract class SetSubstitutionTargetServiceRelationshipProcessor {
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private TopologyService topologyService;

    public void process(Csar csar, Topology topology, SubstitutionTarget substitutionTarget, String relationshipType, String relationshipVersion) {
        if (StringUtils.isBlank(relationshipType)) {
            substitutionTarget.setServiceRelationshipType(null);
            return;
        }

        // Check that the relationship type exists
        RelationshipType indexedRelationshipType = toscaTypeSearchService.find(RelationshipType.class, relationshipType, relationshipVersion);
        if (indexedRelationshipType == null) {
            throw new NotFoundException(RelationshipType.class.getName(), relationshipType + ":" + relationshipVersion,
                    "Unable to find relationship type to create template in topology.");
        }

        topologyService.loadType(topology, indexedRelationshipType);

        substitutionTarget.setServiceRelationshipType(relationshipType);
    }
}