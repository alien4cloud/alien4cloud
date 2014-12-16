package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.*;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.*;

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

        callRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, REMOVE_SOURCE, REMOVE_TARGET);

        call(node, STANDARD, DELETE);
        state(node.getId(), DELETED);
    }
}