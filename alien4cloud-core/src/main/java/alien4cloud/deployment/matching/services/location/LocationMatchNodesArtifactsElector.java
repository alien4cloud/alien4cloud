package alien4cloud.deployment.matching.services.location;

import java.util.Set;

import alien4cloud.model.orchestrators.ArtifactSupport;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.Sets;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.deployment.matching.ILocationMatch;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.tosca.ToscaUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * LocationMatch Elector based on supported artifacts. Checks if the artifacts of a given {@link NodeTemplate} are supported by the location's orchestrator.
 *
 */
@Component
public class LocationMatchNodesArtifactsElector {

    /**
     * Perform matching of the given match context to ensure that a given node template's implementation artifact types are indeed suported by the ones of the
     * context location match.
     * 
     * @param matchContext
     * @return
     */
    public boolean isEligible(LocationMatchNodeFilter.NodeMatchContext matchContext) {
        boolean isEligible = true;
        if (matchContext.getTemplate() == null) {
            return isEligible;
        }
        // first check the node interfaces operations artifacts are supported
        isEligible = isEligible(matchContext.getTemplate(), matchContext);
        if (isEligible) {
            // then check relationships interfaces
            isEligible = areRelationshipsArtifactSupported(matchContext);
        }

        return isEligible;
    }

    private boolean isEligible(AbstractTemplate template, LocationMatchNodeFilter.NodeMatchContext matchContext) {
        if (template == null) {
            return true;
        }

        ArtifactSupport artifactSupport = matchContext.getArtifactSupport();

        // if no supported artifact defined, then return true
        if (artifactSupport == null || ArrayUtils.isEmpty(artifactSupport.getTypes())) {
            return true;
        }
        String[] supportedArtifacts = artifactSupport.getTypes();

        IndexedArtifactToscaElement indexedArtifactToscaElement = matchContext.getElement(IndexedArtifactToscaElement.class, template.getType());

        if (MapUtils.isNotEmpty(indexedArtifactToscaElement.getInterfaces())) {
            for (Interface interfaz : indexedArtifactToscaElement.getInterfaces().values()) {
                for (Operation operation : interfaz.getOperations().values()) {
                    if (operation.getImplementationArtifact() != null) {
                        String artifactTypeString = operation.getImplementationArtifact().getArtifactType();

                        IndexedArtifactType artifactType = matchContext.getElement(IndexedArtifactType.class, artifactTypeString);

                        // stop the checking once one artifactType is not supported
                        if (!isFromOneOfTypes(supportedArtifacts, artifactType)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean isFromOneOfTypes(String[] supportedArtifacts, IndexedArtifactType artifactType) {
        for (String supportedArtifact : supportedArtifacts) {
            if (ToscaUtils.isFromType(supportedArtifact, artifactType)) {
                return true;
            }
        }
        return false;
    }

    private boolean areRelationshipsArtifactSupported(LocationMatchNodeFilter.NodeMatchContext matchContext) {
        if (MapUtils.isNotEmpty(matchContext.getTemplate().getRelationships())) {
            for (RelationshipTemplate relTemplate : matchContext.getTemplate().getRelationships().values()) {
                if (!isEligible(relTemplate, matchContext)) {
                    return false;
                }
            }
        }

        return true;
    }
}
