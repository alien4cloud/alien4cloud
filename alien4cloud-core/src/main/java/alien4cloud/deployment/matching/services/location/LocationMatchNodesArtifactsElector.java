package alien4cloud.deployment.matching.services.location;

import alien4cloud.model.orchestrators.ArtifactSupport;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.templates.AbstractTemplate;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.tosca.ToscaUtils;
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

        AbstractInstantiableToscaType indexedArtifactToscaElement = matchContext.getElement(AbstractInstantiableToscaType.class, template.getType());

        if (MapUtils.isNotEmpty(indexedArtifactToscaElement.getInterfaces())) {
            for (Interface interfaz : indexedArtifactToscaElement.getInterfaces().values()) {
                for (Operation operation : interfaz.getOperations().values()) {
                    if (operation.getImplementationArtifact() != null) {
                        String artifactTypeString = operation.getImplementationArtifact().getArtifactType();

                        ArtifactType artifactType = matchContext.getElement(ArtifactType.class, artifactTypeString);

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

    private boolean isFromOneOfTypes(String[] supportedArtifacts, ArtifactType artifactType) {
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
