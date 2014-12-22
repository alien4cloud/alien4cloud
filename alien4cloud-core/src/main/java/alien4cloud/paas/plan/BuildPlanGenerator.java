package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.*;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.*;
import alien4cloud.paas.model.PaaSNodeTemplate;

/**
 * Generates the default tosca build plan.
 */
public class BuildPlanGenerator extends AbstractPlanGenerator {
    @Override
    protected void generateNodeWorkflow(PaaSNodeTemplate node) {
        state(node.getId(), INITIAL);

        call(node, STANDARD, CREATE);
        state(node.getId(), CREATED);

        waitTarget(node, DEPENDS_ON, STARTED);

        callRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, PRE_CONFIGURE_SOURCE, PRE_CONFIGURE_TARGET);

        call(node, STANDARD, ToscaNodeLifecycleConstants.CONFIGURE);
        state(node.getId(), CONFIGURED);

        callRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, POST_CONFIGURE_SOURCE, POST_CONFIGURE_TARGET);

        call(node, STANDARD, START);
        state(node.getId(), STARTED);

        waitSource(node, CONNECTS_TO, STARTED);

        // synchronous add source / target implementation.
        callRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, ADD_TARGET, ADD_SOURCE);

        state(node.getId(), AVAILABLE);

        // custom alien support of sequence hosted on.
        if(node.isCreateChildrenSequence()) {
            sequencial(node.getChildren());
        } else {
            // process child nodes.
            parallel(node.getChildren());
        }
    }
}