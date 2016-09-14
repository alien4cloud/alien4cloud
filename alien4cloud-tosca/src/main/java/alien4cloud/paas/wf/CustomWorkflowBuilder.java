package alien4cloud.paas.wf;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.paas.wf.WorkflowsBuilderService.TopologyContext;
import alien4cloud.paas.wf.exception.BadWorkflowOperationException;

@Component
@Slf4j
public class CustomWorkflowBuilder extends AbstractWorkflowBuilder {

    @Override
    public void addNode(Workflow wf, String nodeId, TopologyContext toscaTypeFinder, boolean isCompute) {
        // nodes are not added to custom workflows

    }

    @Override
    public void addRelationship(Workflow wf, String nodeId, NodeTemplate nodeTemplate, RelationshipTemplate RelationshipTemplate,
            TopologyContext toscaTypeFinder) {
        // relationships are not added to custom workflows
    }

    @Override
    public Workflow reinit(Workflow wf, TopologyContext toscaTypeFinder) {
        throw new BadWorkflowOperationException(String.format("Reinit can not be performed on non standard workflow '%s'", wf.getName()));
    }

}
