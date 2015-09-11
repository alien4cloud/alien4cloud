package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;

@Component
@Slf4j
public class CustomWorkflowBuilder extends AbstractWorkflowBuilder {

    @Override
    public void addNode(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, boolean isCompute) {
        // nodes are not added to custom workflows
    }

    @Override
    public void addRelationship(Workflow wf, PaaSTopology paaSTopology, PaaSNodeTemplate paaSNodeTemplate, PaaSRelationshipTemplate pasSRelationshipTemplate) {
        // relationships are not added to custom workflows
    }

    @Override
    public Workflow reinit(Workflow wf, Topology topology, PaaSTopology paaSTopology) {
        throw new BadWorkflowOperationException(String.format("Reinit can not be performed on non standard workflow '%s'", wf.getName()));
    }

}
