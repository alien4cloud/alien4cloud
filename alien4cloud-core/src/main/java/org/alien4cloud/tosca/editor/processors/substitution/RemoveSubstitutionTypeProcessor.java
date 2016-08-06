package org.alien4cloud.tosca.editor.processors.substitution;

import static alien4cloud.utils.AlienUtils.safe;

import alien4cloud.csar.services.CsarService;
import alien4cloud.exception.DeleteReferencedObjectException;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import org.alien4cloud.tosca.editor.EditionContextManager;
import org.alien4cloud.tosca.editor.operations.substitution.RemoveSubstitutionTypeOperation;
import org.alien4cloud.tosca.editor.processors.IEditorOperationProcessor;
import org.springframework.stereotype.Component;

import alien4cloud.exception.NotFoundException;
import alien4cloud.model.topology.Topology;

import javax.annotation.Resource;

/**
 * Delete the substitute of a topology template.
 */
@Component
public class RemoveSubstitutionTypeProcessor implements IEditorOperationProcessor<RemoveSubstitutionTypeOperation> {

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private CsarService csarService;


    @Override
    public void process(RemoveSubstitutionTypeOperation operation) {
        Topology topology = EditionContextManager.getTopology();
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            throw new NotFoundException("No substitution type has been found");
        }
        IndexedNodeType substitutionType = topology.getSubstitutionMapping().getSubstitutionType();
        Csar csar = ToscaContext.get().getArchiveByTopologyId(operation.getTopologyId());
        if (csar != null) {
            Topology[] topologies = csarService.getDependantTopologies(csar.getName(), csar.getVersion());
            if (topologies != null) {
                for (Topology topologyThatUseCsar : topologies) {
                    if (!topologyThatUseCsar.getId().equals(operation.getTopologyId())) {
                        throw new DeleteReferencedObjectException(
                                "The substitution can not be removed since it's type is already used in at least another topology");
                    }
                }
            }
            Csar[] dependantCsars = csarService.getDependantCsars(csar.getName(), csar.getVersion());
            if (dependantCsars != null && dependantCsars.length > 0) {
                throw new DeleteReferencedObjectException("The substitution can not be removed since it's a dependency for another csar");
            }
            csar.setSubstitutionTopologyId(null);
        }
        topologyService.unloadType(topology, new String[] { substitutionType.getElementId() });
        topology.setSubstitutionMapping(null);
    }
}