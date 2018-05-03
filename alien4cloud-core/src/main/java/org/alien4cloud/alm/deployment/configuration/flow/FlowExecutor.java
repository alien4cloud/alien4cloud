package org.alien4cloud.alm.deployment.configuration.flow;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.modifiers.CfyMultirelationshipErrorModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.EditorTopologyValidator;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhaseModifiersExecutor;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.FlowPhases;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.LocationMatchingModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingAbstractValidator;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingNodeSetupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingPolicySetupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PreDeploymentTopologyValidator;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.SubstitutionCompositionModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs.InputArtifactsModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs.InputValidationModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs.InputsModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.inputs.PreconfiguredInputsModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingCandidateModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingCompositeModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingConfigCleanupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.NodeMatchingReplaceModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingCandidateModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingCompositeModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingConfigCleanupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingReplaceModifier;
import org.alien4cloud.alm.deployment.configuration.services.DeploymentConfigurationDao;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.tosca.context.ToscaContextual;
import lombok.extern.slf4j.Slf4j;

/**
 * Execute a flow in order to convert a topology into a ready to deploy topology. Flow consist of the processing of multiple Topology Modifiers that takes a
 * Topology as an input and create a resulting Topology.
 *
 * Usual flow consist of the following operations:
 * <ul>
 * <li>Apply topology specific environment variables</li>
 * <li>Validation of the Topology to be ready to pass to deployer user</li>
 * <li>Location matching</li>
 * <li>Apply deployer inputs and applications and environment meta-properties and process get_property functions</li>
 * <li>Apply injected location modifiers if any</li>
 * <li>Policy matching</li>
 * <li>Apply injected policy modifiers to</li>
 * <li>Validation that no required inputs are missing</li>
 * <li>Apply node matching</li>
 * <li>Apply node override configurations</li>
 * <li>Validation that no required properties are missing</li>
 * </ul>
 *
 * Note that any flow element may interrupt the flow if some errors are triggered. Any flow element may also add some warnings.
 */
@Slf4j
@Component
public class FlowExecutor {

    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private SubstitutionCompositionModifier substitutionCompositionModifier;
    @Inject
    private EditorTopologyValidator editorTopologyValidator;

    @Inject
    private LocationMatchingModifier locationMatchingModifier;
    // To be moved to location specific
    @Inject
    private CfyMultirelationshipErrorModifier cfyMultirelationshipErrorModifier;

    @Inject
    private PreconfiguredInputsModifier preconfiguredInputsModifier;
    @Inject
    private InputsModifier inputsModifier;
    @Inject
    private InputArtifactsModifier inputArtifactsModifier;
    @Inject
    private InputValidationModifier inputValidationModifier;

    @Inject
    private PolicyMatchingCandidateModifier policyMatchingCandidateModifier;
    @Inject
    private PolicyMatchingConfigCleanupModifier policyMatchingConfigCleanupModifier;
    @Inject
    private PolicyMatchingConfigAutoSelectModifier policyMatchingConfigAutoSelectModifier;
    @Inject
    private PolicyMatchingReplaceModifier policyMatchingReplaceModifier;

    @Inject
    private NodeMatchingCandidateModifier nodeMatchingCandidateModifier;
    @Inject
    private NodeMatchingConfigCleanupModifier nodeMatchingConfigCleanupModifier;
    @Inject
    private NodeMatchingConfigAutoSelectModifier nodeMatchingConfigAutoSelectModifier;
    @Inject
    private NodeMatchingReplaceModifier nodeMatchingReplaceModifier;

    @Inject
    private PostMatchingAbstractValidator postMatchingAbstractValidator;

    @Inject
    private PostMatchingPolicySetupModifier postMatchingPolicySetupModifier;
    @Inject
    private PostMatchingNodeSetupModifier postMatchingNodeSetupModifier;

    @Inject
    private PreDeploymentTopologyValidator preDeploymentTopologyValidator;

    private List<ITopologyModifier> topologyModifiers;

    @PostConstruct
    private void initModifiers() {
        topologyModifiers = getDefaultFlowModifiers();
    }

    /**
     * Get the default list of topology modifiers.
     *
     * @return The default list of topology modifiers.
     */
    public List<ITopologyModifier> getDefaultFlowModifiers() {
        List<ITopologyModifier> topologyModifiers = Lists.newArrayList();
        // Future: process pre-environment topology executors
        // Future: Process environment in-topology variables (different from inputs as configured by the topology editor)
        // Process topology compositions (in case of usage of substitution nodes)
        topologyModifiers.add(substitutionCompositionModifier);
        // Process topology validation before actually letting the deployer to configure deployment
        topologyModifiers.add(editorTopologyValidator);
        // just process inputs variables to make them available before location match
        topologyModifiers.add(preconfiguredInputsModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_LOCATION_MATCH));
        // Checks location matching
        topologyModifiers.add(locationMatchingModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_LOCATION_MATCH));
        // FIXME cfy specific modifier, remove when issue solved or move to cfy plugin when possible
        topologyModifiers.add(cfyMultirelationshipErrorModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_INJECT_INPUT));
        // Inject preconfigured inputs (reading inputs file mapping) into the configuration context
        topologyModifiers.add(preconfiguredInputsModifier);
        // Inject deployer + preconfigured + location inputs in the topology. This is done after location matching as we may have inputs that refers to location
        // meta properties.
        topologyModifiers.add(inputsModifier);
        // Inject input artifacts in the topology.
        topologyModifiers.add(inputArtifactsModifier);
        // Process validation of constraints, and that no required inputs are missing
        topologyModifiers.add(inputValidationModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_INJECT_INPUT));
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_POLICY_MATCH));
        topologyModifiers.add(new PolicyMatchingCompositeModifier(policyMatchingCandidateModifier, // Find candidate matches.
                policyMatchingConfigCleanupModifier, // cleanup configuriton in case some matches are not valid anymore.
                policyMatchingConfigAutoSelectModifier, // performs auto-selection of policies impl.
                policyMatchingReplaceModifier// Inject policy implementation modifiers in the flow and eventually add warnings.
        ));
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_POLICY_MATCH));

        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_MATCHED_POLICY_SETUP));
        // Overrides unspecified matched/substituted policies unset's properties with values provided by the deployer user
        topologyModifiers.add(postMatchingPolicySetupModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_MATCHED_POLICY_SETUP));

        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_NODE_MATCH));
        // Future: Load specific pre-matching location specific modifiers (pre-matching policy handlers etc.)
        // Node matching is composed of multiple sub modifiers that performs the various steps of matching.
        topologyModifiers.add(new NodeMatchingCompositeModifier(nodeMatchingCandidateModifier, // find matching candidates (do not change topology)
                nodeMatchingConfigCleanupModifier, // cleanup user configuration if some config are not valid anymore
                nodeMatchingConfigAutoSelectModifier, // auto-select missing nodes
                nodeMatchingReplaceModifier // Impact the topology to replace matched nodes as configured
        ));
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_NODE_MATCH));

        topologyModifiers.add(postMatchingAbstractValidator);

        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.PRE_MATCHED_NODE_SETUP));
        // Overrides unspecified matched/substituted nodes unset's properties with values provided by the deployer user
        topologyModifiers.add(postMatchingNodeSetupModifier);
        topologyModifiers.add(new FlowPhaseModifiersExecutor(FlowPhases.POST_MATCHED_NODE_SETUP));
        // Future: Load specific post-matching location specific modifiers (Security groups additions/ Auto-configuration, Pre-deployment policies handlers
        // etc..)
        // Check orchestrator properties configuration

        // Perform full pre-deployment validation
        topologyModifiers.add(preDeploymentTopologyValidator);
        return topologyModifiers;
    }

    /**
     * Execute deployment modifier flow in the context of an environment.
     *
     * @param topology The topology that will be impacted by the flow.
     * @param application The application that owns the topology.
     * @param environment The environment for which to execute deployment flow.
     * @return The context of execution that contains the updated topology as well as logs and cached elements from the modifiers.
     */
    @ToscaContextual
    public FlowExecutionContext executeDeploymentFlow(Topology topology, Application application, ApplicationEnvironment environment) {
        FlowExecutionContext executionContext = new FlowExecutionContext(deploymentConfigurationDao, topology,
                new EnvironmentContext(application, environment));
        execute(topologyModifiers, executionContext);
        return executionContext;
    }

    @ToscaContextual
    public void execute(Topology topology, List<ITopologyModifier> modifiers, FlowExecutionContext context) {
        execute(modifiers, context);
    }

    private void execute(List<ITopologyModifier> modifiers, FlowExecutionContext context) {
        for (int i = 0; i < modifiers.size(); i++) {
            long start = System.currentTimeMillis();
            modifiers.get(i).process(context.getTopology(), context);
            log.debug("Processed <" + modifiers.get(i).getClass().getSimpleName() + "> in " + (System.currentTimeMillis() - start) + " ms");
            if (!context.log().isValid()) {
                // In case of errors we don't process the flow further.
                return;
            }
        }
    }
}