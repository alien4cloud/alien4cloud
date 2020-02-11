package org.alien4cloud.alm.deployment.configuration.flow.modifiers.matching;

import java.util.List;
import java.util.Map;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.springframework.stereotype.Component;

import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;

/**
 * This modifier cleanup the user matching configuration in case it is not valid anymore based on the choices available (that must be fetched from prior
 * NodeMatchingCandidateModifier execution).
 *
 * It does not update topology or matching configurations, these operations are done in sub-sequent modifiers.
 */
@Component
public class NodeMatchingConfigCleanupModifier extends AbstractMatchingConfigCleanupModifier<LocationResourceTemplate> {

    @Override
    protected Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedLocationResources();
    }

    @Override
    protected Map<String, List<LocationResourceTemplate>> getAvailableMatches(FlowExecutionContext context) {
        return (Map<String, List<LocationResourceTemplate>>) context.getExecutionCache().get(FlowExecutionContext.MATCHED_NODE_LOCATION_TEMPLATES_BY_NODE_ID_MAP);
    }
}