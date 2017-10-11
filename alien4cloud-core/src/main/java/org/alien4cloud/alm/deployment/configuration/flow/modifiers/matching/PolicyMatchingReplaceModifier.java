package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.topology.task.LocationPolicyTask;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Last policy matching modifier, it actually inject policy modifiers implementations as policies implementations may impact the actual topology to be deployed.
 */
@Component
public class PolicyMatchingReplaceModifier implements ITopologyModifier {
    @Override
    public void process(Topology topology, FlowExecutionContext context) {
        Optional<DeploymentMatchingConfiguration> configurationOptional = context.getConfiguration(DeploymentMatchingConfiguration.class,
                NodeMatchingReplaceModifier.class.getSimpleName());

        if (!configurationOptional.isPresent()) { // we should not end-up here as location matching should be processed first
            context.log().error(new LocationPolicyTask());
            return;
        }

        DeploymentMatchingConfiguration matchingConfiguration = configurationOptional.get();
        Map<String, LocationResourceTemplate> allAvailableResourceTemplate = (Map<String, LocationResourceTemplate>) context.getExecutionCache()
                .get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_ID_MAP);

        Map<String, String> lastUserSubstitutions = matchingConfiguration.getMatchedLocationResources();

        // If some policies have not been matched a warning is logged (policy support is considered as warning but does not prevent deployment).

    }
}