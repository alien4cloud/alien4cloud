package org.alien4cloud.alm.deployment.configuration.flow.modifiers.action;

import java.util.Map;
import java.util.Set;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;

import alien4cloud.orchestrators.locations.services.ILocationResourceService;

public class SetMatchedNodeModifier extends AbstractSetMatchedModifier {

    public SetMatchedNodeModifier(String nodeId, String locationResourceTemplateId, ILocationResourceService locationResourceService) {
        super(nodeId, locationResourceTemplateId, locationResourceService);
    }

    @Override
    Map<String, String> getLastUserMatches(DeploymentMatchingConfiguration matchingConfiguration) {
        return matchingConfiguration.getMatchedLocationResources();
    }

    @Override
    Map<String, Set<String>> getAvailableSubstitutions(FlowExecutionContext context) {
        return (Map<String, Set<String>>) context.getExecutionCache()
                .get(FlowExecutionContext.SELECTED_MATCH_NODE_LOCATION_TEMPLATE_BY_NODE_ID_MAP);
    }

    @Override
    String getSubject() {
        return "node";
    }

}