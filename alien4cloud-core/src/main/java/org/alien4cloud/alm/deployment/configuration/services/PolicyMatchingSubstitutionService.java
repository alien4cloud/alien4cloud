package org.alien4cloud.alm.deployment.configuration.services;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Maps;

import org.alien4cloud.alm.deployment.configuration.events.OnDeploymentConfigCopyEvent;
import org.alien4cloud.alm.deployment.configuration.flow.EnvironmentContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutor;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.action.SetMatchedPolicyModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.AbstractComposedModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingCompositeModifier;
import org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching.PolicyMatchingConfigAutoSelectModifier;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import alien4cloud.application.ApplicationService;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.topology.TopologyServiceCore;

/**
 * Service responsible to configure the matched substitutions on policies for a given deployment.
 */
@Service
public class PolicyMatchingSubstitutionService {
    @Inject
    private DeploymentConfigurationDao deploymentConfigurationDao;
    @Inject
    private FlowExecutor flowExecutor;
    @Inject
    private PolicyMatchingConfigAutoSelectModifier policyMatchingConfigAutoSelectModifier;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private ApplicationService applicationService;

    /**
     * Execute the deployment flow with a modification of changing the substitution for one of the nodes.
     *
     * @param application The application for which to execute the deployment flow.
     * @param environment The environment for which to execute the deployment flow.
     * @param topology The topology linked to the specified environment.
     * @param nodeId The id of the node to substitute at matching phase.
     * @param resourceTemplateId The id of the location resource to substitute.
     * @return The flow execution context.
     */
    public FlowExecutionContext updateSubstitution(Application application, ApplicationEnvironment environment, Topology topology, String nodeId,
            String resourceTemplateId) {
        FlowExecutionContext executionContext = new FlowExecutionContext(deploymentConfigurationDao, topology,
                new EnvironmentContext(application, environment));
        // Load the actual configuration

        // add a modifier that will actually perform the configuration of a substitution from user request (after cleanup and prior to node matching
        // auto-selection).
        SetMatchedPolicyModifier setMatchedPolicyModifier = new SetMatchedPolicyModifier(nodeId, resourceTemplateId);
        List<ITopologyModifier> modifierList = getModifierListWithSelectionAction(setMatchedPolicyModifier);

        flowExecutor.execute(topology, modifierList, executionContext);
        if (!setMatchedPolicyModifier.isExecuted()) {
            throw new NotFoundException("Requested substitution <" + resourceTemplateId + "> for node <" + nodeId
                    + "> is not available as a match. Please check that inputs and location are configured and ask your admin to grant you access to requested resources on the location.");
        }

        return executionContext;
    }

    private List<ITopologyModifier> getModifierListWithSelectionAction(SetMatchedPolicyModifier matchedModifier) {
        List<ITopologyModifier> modifierList = flowExecutor.getDefaultFlowModifiers();
        PolicyMatchingCompositeModifier matchingModifier = (PolicyMatchingCompositeModifier) modifierList.stream()
                .filter(modifier -> modifier instanceof PolicyMatchingCompositeModifier).findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Unexpected exception in deployment flow to update node substitution; unable to find the master node matching modifier to inject selection action modifier."));

        // inject the SetMatchedNodeModifier into the nodeMatchingModifiers, just after policyMatchingConfigAutoSelectModifier
        matchingModifier.addModifierBefore(matchedModifier, policyMatchingConfigAutoSelectModifier);
        return modifierList;
    }

    // FIXME fix this, synch with org.alien4cloud.alm.deployment.configuration.services.MatchingSubstitutionService#onCopyConfiguration
    // @EventListener
    // @Order(30) // Process this after location matching copy (first element).
    public void onCopyConfiguration(OnDeploymentConfigCopyEvent onDeploymentConfigCopyEvent) {
        ApplicationEnvironment source = onDeploymentConfigCopyEvent.getSourceEnvironment();
        ApplicationEnvironment target = onDeploymentConfigCopyEvent.getTargetEnvironment();
        DeploymentMatchingConfiguration sourceConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(source.getTopologyVersion(), source.getId()));

        DeploymentMatchingConfiguration targetConfiguration = deploymentConfigurationDao.findById(DeploymentMatchingConfiguration.class,
                AbstractDeploymentConfig.generateId(target.getTopologyVersion(), target.getId()));

        if (sourceConfiguration == null || MapUtils.isEmpty(sourceConfiguration.getLocationGroups()) || targetConfiguration == null
                || MapUtils.isEmpty(targetConfiguration.getLocationGroups())) {
            return; // Nothing to copy
        }

        // We have to execute a piece of the deployment flow to find out matching candidates so we copy only required inputs
        Topology topology = topologyServiceCore.getOrFail(Csar.createId(target.getApplicationId(), target.getTopologyVersion()));

        if (MapUtils.isNotEmpty(topology.getNodeTemplates())) {
            Application application = applicationService.getOrFail(target.getApplicationId());
            FlowExecutionContext executionContext = new FlowExecutionContext(deploymentConfigurationDao, topology, new EnvironmentContext(application, target));
            flowExecutor.execute(topology, getMatchingFlow(), executionContext);

            Map<String, Set<String>> locResTemplateIdsPerNodeIds = (Map<String, Set<String>>) executionContext.getExecutionCache()
                    .get(FlowExecutionContext.SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP);

            // Update the substitution on the target if available substitution is always compatible
            Map<String, String> validOnNewEnvSubstitutedNodes = safe(sourceConfiguration.getMatchedLocationResources()).entrySet().stream()
                    .filter(entry -> locResTemplateIdsPerNodeIds.containsKey(entry.getKey())
                            && locResTemplateIdsPerNodeIds.get(entry.getKey()).contains(entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (MapUtils.isNotEmpty(validOnNewEnvSubstitutedNodes)) {

                if (targetConfiguration.getMatchedLocationResources() == null) {
                    targetConfiguration.setMatchedLocationResources(Maps.newHashMap());
                }

                validOnNewEnvSubstitutedNodes.forEach((key, value) -> {
                    targetConfiguration.getMatchedLocationResources().put(key, value);
                    // Copy properties set on the node to the new one
                    targetConfiguration.getMatchedNodesConfiguration().put(key, safe(sourceConfiguration.getMatchedNodesConfiguration()).get(key));
                });

                deploymentConfigurationDao.save(targetConfiguration);
            }
        }
    }

    private List<ITopologyModifier> getMatchingFlow() {
        List<ITopologyModifier> modifierList = flowExecutor.getDefaultFlowModifiers();
        // only keep modifiers until MatchingModifier
        for (int i = 0; i < modifierList.size(); i++) {
            if (modifierList.get(i) instanceof PolicyMatchingCompositeModifier) {
                // FIXME what shoud we do regarding node modifers?
                // only keep policy matching modifiers until policyMatchingConfigAutoSelectModifier
                ((AbstractComposedModifier) modifierList.get(i)).removeModifiersAfter(policyMatchingConfigAutoSelectModifier);
                return modifierList.subList(0, i + 1);
            }
        }

        throw new IllegalArgumentException("Unexpected exception in deployment flow to update node substitution; unable to find the "
                + AbstractComposedModifier.class.getSimpleName() + " modifier to proceed.");
    }
}