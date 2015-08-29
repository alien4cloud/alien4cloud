package alien4cloud.paas.plan;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.AVAILABLE;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CONFIGURED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CREATE;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CREATED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.INITIAL;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STANDARD;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.START;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STARTED;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.ADD_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.ADD_TARGET;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.POST_CONFIGURE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.POST_CONFIGURE_TARGET;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_TARGET;
import static alien4cloud.tosca.normative.NormativeRelationshipConstants.CONNECTS_TO;
import static alien4cloud.tosca.normative.NormativeRelationshipConstants.DEPENDS_ON;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import org.apache.commons.collections4.CollectionUtils;

import alien4cloud.paas.model.PaaSNodeTemplate;

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

        if (includeAttached && CollectionUtils.isNotEmpty(node.getStorageNodes())) {
            sequencial(node.getStorageNodes());
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