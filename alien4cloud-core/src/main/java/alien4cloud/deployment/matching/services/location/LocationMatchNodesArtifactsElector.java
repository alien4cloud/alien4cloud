package alien4cloud.deployment.matching.services.location;

import alien4cloud.model.deployment.matching.LocationMatch;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.collections4.MapUtils;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;

/**
 * Elector for a NodeTemplate. Checks if the artifacts defined in both the nodes and related relationships interfaces operation are supported by the location's
 * orchestrator.
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class LocationMatchNodesArtifactsElector extends AbstractLocationMatchArtifactsElector {

    @Override
    public boolean isEligible(LocationMatch locationMatch) {
        boolean isEligible = true;
        NodeTemplate nodeTemplate = (NodeTemplate) getTemplate();
        if (nodeTemplate == null) {
            return isEligible;
        }
        // first check the node interfaces operations artifacts are supported
        isEligible = super.isEligible(locationMatch);
        if (isEligible) {
            // then check relationships interfaces
            isEligible = areRelationshipsArtifactSupported(locationMatch, nodeTemplate);
        }

        return isEligible;
    }

    private boolean areRelationshipsArtifactSupported(LocationMatch locationMatch, NodeTemplate nodeTemplate) {
        if (MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
            for (RelationshipTemplate relTemplate : nodeTemplate.getRelationships().values()) {
                this.setTemplate(relTemplate);
                if (!super.isEligible(locationMatch)) {
                    return false;
                }
            }
        }

        return true;
    }

}
