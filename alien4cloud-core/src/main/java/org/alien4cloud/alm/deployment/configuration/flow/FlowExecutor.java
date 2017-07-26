package org.alien4cloud.alm.deployment.configuration.flow;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.modifiers.EditorTopologyValidator;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.InputArtifactsModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.InputValidationModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.InputsModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.LocationMatchingModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.NodeMatchingCandidateModifer;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.NodeMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.NodeMatchingConfigCleanupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.NodeMatchingReplaceModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PostMatchingNodeSetupModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.PreDeploymentTopologyValidator;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.SubstitutionCompositionModifier;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.tosca.context.ToscaContextual;

/**
 * Execute a flow in order to convert a topology into a ready to deploy topology. Flow consist of the processing of multiple Topology Modifiers that takes a
 * Topology as an input and create a resulting Topology.
 *
 * Usual flow consist of the following operations:
 * <ul>
 * <li>Apply topology specific environment variables</li>
 * <li>Validation of the Topology to be ready to pass to deployer user</li>
 * <li>Apply location matching</li>
 * <li>Apply deployer inputs and applications and environment meta-properties and process get_property functions</li>
 * <li>Validation that no required inputs are missing</li>
 * <li>Apply node matching</li>
 * <li>Apply node override configurations</li>
 * <li>Validation that no required properties are missing</li>
 * </ul>
 * 
 * Note that any flow element may interrupt the flow if some errors are triggered. Any flow element may also add some warnings.
 */
@Component
public class FlowExecutor {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private EditorTopologyValidator editorTopologyValidator;
    @Inject
    private SubstitutionCompositionModifier substitutionCompositionModifier;
    @Inject
    private InputsModifier inputsModifier;
    @Inject
    private InputArtifactsModifier inputArtifactsModifier;
    @Inject
    private LocationMatchingModifier locationMatchingModifier;
    @Inject
    private InputValidationModifier inputValidationModifier;
    @Inject
    private NodeMatchingCandidateModifer nodeMatchingCandidateModifer;
    @Inject
    private NodeMatchingConfigCleanupModifier nodeMatchingConfigCleanupModifier;
    @Inject
    private NodeMatchingConfigAutoSelectModifier nodeMatchingConfigAutoSelectModifier;
    @Inject
    private NodeMatchingReplaceModifier nodeMatchingReplaceModifier;
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
        // Checks location matching
        topologyModifiers.add(locationMatchingModifier);
        // Inject inputs in the topology. This is done after location matching as we may have inputs that refers to location meta properties.
        topologyModifiers.add(inputsModifier);
        // Inject input artifacts in the topology.
        topologyModifiers.add(inputArtifactsModifier);
        // Process validation that no required inputs are missing
        topologyModifiers.add(inputValidationModifier);
        // Future: Load specific pre-matching location specific modifiers (pre-matching policy handlers etc.)
        // Node matching is composed of multiple modifiers that performs the various steps of matching.
        topologyModifiers.add(nodeMatchingCandidateModifer); // find matching candidates (do not change topology)
        topologyModifiers.add(nodeMatchingConfigCleanupModifier); // cleanup user configuration if some config are not valid anymore
        topologyModifiers.add(nodeMatchingConfigAutoSelectModifier); // auto-select missing nodes
        topologyModifiers.add(nodeMatchingReplaceModifier); // Impact the topology to replace matched nodes as configured

        // Overrides unspecified matched/substituted nodes unset's properties with values provided by the deployer user
        topologyModifiers.add(postMatchingNodeSetupModifier);

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
        FlowExecutionContext executionContext = new FlowExecutionContext(alienDAO, topology, new EnvironmentContext(application, environment));
        execute(topologyModifiers, executionContext);
        return executionContext;
    }

    @ToscaContextual
    public void execute(Topology topology, List<ITopologyModifier> modifiers, FlowExecutionContext context) {
        execute(modifiers, context);
    }

    private void execute(List<ITopologyModifier> modifiers, FlowExecutionContext context) {
        for (int i = 0; i < modifiers.size(); i++) {
            modifiers.get(i).process(context.getTopology(), context);
            if (!context.log().isValid()) {
                // In case of errors we don't process the flow further.
                return;
            }
        }
    }
}