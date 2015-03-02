package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.*;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.*;
import static alien4cloud.tosca.normative.NormativeRelationshipConstants.CONNECTS_TO;
import static alien4cloud.tosca.normative.NormativeRelationshipConstants.DEPENDS_ON;

import alien4cloud.paas.model.PaaSNodeTemplate;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Generates the default tosca build plan.
 */
@NoArgsConstructor
@AllArgsConstructor
public class BuildPlanGenerator extends AbstractPlanGenerator {
    private boolean includeAttached = false;

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
        // in case the relationships is a dependency on myself the only status i will wait is STARTED before I call the add target and add source
        waitMyself(node, DEPENDS_ON, STARTED);

        // synchronous add source / target implementation.
        // trigger them
        triggerRelations(node, ToscaRelationshipLifecycleConstants.CONFIGURE, ADD_SOURCE, ADD_TARGET);

        state(node.getId(), AVAILABLE);

        if (includeAttached && node.getAttachedNode() != null) {
            sequencial(Lists.newArrayList(node.getAttachedNode()));
        }

        // custom alien support of sequence hosted on.
        if (node.isCreateChildrenSequence()) {
            sequencial(node.getChildren());
        } else {
            // process child nodes.
            parallel(node.getChildren());
        }
    }
}