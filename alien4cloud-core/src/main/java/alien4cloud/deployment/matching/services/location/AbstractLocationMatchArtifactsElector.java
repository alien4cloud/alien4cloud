package alien4cloud.deployment.matching.services.location;

import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.model.deployment.matching.LocationMatch;
import alien4cloud.orchestrators.services.OrchestratorService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.Interface;
import alien4cloud.model.components.Operation;
import alien4cloud.model.topology.AbstractTemplate;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorFactoriesRegistry;
import alien4cloud.tosca.ToscaUtils;

import com.google.common.collect.Sets;

/**
 * LocationMatch Elector based on supported artifacts. Checks if the artifacts of a given {@link AbstractTemplate} are supported by the location's orchestrator.
 *
 * @author 'Igor Ngouagna'
 *
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractLocationMatchArtifactsElector implements ILocationMatchElector {

    // TypeMap cache = new TypeMap();

    /**
     * Template to check artifacts
     */
    private AbstractTemplate template;

    /**
     * dependencies in which to look for related {@link IndexedArtifactToscaElement}
     */
    private Set<CSARDependency> dependencies = Sets.newHashSet();

    @Resource
    private CSARRepositorySearchService csarSearchService;
    @Resource
    private OrchestratorService orchestratorService;

    @Override
    public boolean isEligible(LocationMatch locationMatch) {
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
}
