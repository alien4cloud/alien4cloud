package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.DELETE;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.DELETED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STANDARD;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STOP;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STOPPED;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.REMOVE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.REMOVE_TARGET;
import alien4cloud.paas.model.PaaSNodeTemplate;

/**
 * Generates the default tosca build plan.
 */
public class StopPlanGenerator extends AbstractPlanGenerator {
    @Override
    protected void generateNodeWorkflow(PaaSNodeTemplate node) {
        // process child nodes.
        parallel(node.getChildren());

        call(node, STANDARD, STOP);
        state(node.getId(), STOPPED);

        triggerRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, REMOVE_SOURCE, REMOVE_TARGET);

        call(node, STANDARD, DELETE);
        state(node.getId(), DELETED);
    }
}