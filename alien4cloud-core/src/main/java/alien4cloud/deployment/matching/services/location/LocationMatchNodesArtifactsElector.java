package alien4cloud.deployment.matching.services.location;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.tosca.ToscaUtils;

import com.google.common.collect.Sets;

/**
 * LocationMatch Elector based on supported artifacts. Checks if the artifacts of a given {@link NodeTemplate} are supported by the location's orchestrator.
 *
 */
@Getter
@Setter
public class LocationMatchNodesArtifactsElector implements ILocationMatchElector {

    // TypeMap cache = new TypeMap();

    /**
     * Template to check artifacts
     */
    private NodeTemplate template;

    /**
     * dependencies in which to look for related {@link IndexedArtifactToscaElement}
     */
    private Set<CSARDependency> dependencies = Sets.newHashSet();
    private CSARRepositorySearchService csarSearchService;
    private OrchestratorService orchestratorService;

    @Override
    public boolean isEligible(LocationMatch locationMatch) {
        boolean isEligible = true;
        if (template == null) {
            return isEligible;
        }
        // first check the node interfaces operations artifacts are supported
        isEligible = isEligible(template, locationMatch);
        if (isEligible) {
            // then check relationships interfaces
            isEligible = areRelationshipsArtifactSupported(locationMatch, template);
        }

        return isEligible;
    }

    private boolean isEligible(AbstractTemplate template, LocationMatch locationMatch) {
        if (template == null) {
            return true;
        }

        IOrchestratorPluginFactory orchestratorFactory = orchestratorService.getPluginFactory(locationMatch.getOrchestrator());
        String[] supportedArtifacts = orchestratorFactory.getArtifactSupport().getTypes();

        // if no supported artifact defined, then return true
        if (ArrayUtils.isEmpty(supportedArtifacts)) {
            return true;
        }

        IndexedArtifactToscaElement indexedArtifactToscaElement = csarSearchService.getRequiredElementInDependencies(IndexedArtifactToscaElement.class,
                template.getType(), dependencies);

        if (MapUtils.isNotEmpty(indexedArtifactToscaElement.getInterfaces())) {
            for (Interface interfaz : indexedArtifactToscaElement.getInterfaces().values()) {
                for (Operation operation : interfaz.getOperations().values()) {
                    if (operation.getImplementationArtifact() != null) {
                        String artifactTypeString = operation.getImplementationArtifact().getArtifactType();
                        IndexedArtifactType artifactType = csarSearchService.getElementInDependencies(IndexedArtifactType.class, artifactTypeString,
                                dependencies);

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
        boolean result = false;
        for (int i = 0; (i < supportedArtifacts.length && result == false); i++) {
            result = ToscaUtils.isFromType(supportedArtifacts[i], artifactType);
        }
        return result;
    }

    private boolean areRelationshipsArtifactSupported(LocationMatch locationMatch, NodeTemplate nodeTemplate) {
        if (MapUtils.isNotEmpty(nodeTemplate.getRelationships())) {
            for (RelationshipTemplate relTemplate : nodeTemplate.getRelationships().values()) {
                if (!isEligible(relTemplate, locationMatch)) {
                    return false;
                }
            }
        }

        return true;
    }

    public LocationMatchNodesArtifactsElector(CSARRepositorySearchService csarSearchService, OrchestratorService orchestratorService) {
        this.csarSearchService = csarSearchService;
        this.orchestratorService = orchestratorService;
    }
}
