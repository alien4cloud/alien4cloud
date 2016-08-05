package alien4cloud.dao;

import java.beans.IntrospectionException;
import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.common.AbstractSuggestionEntry;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.common.SimpleSuggestionEntry;
import alien4cloud.model.common.SuggestionEntry;
import alien4cloud.model.components.Csar;
import alien4cloud.model.components.IndexedArtifactToscaElement;
import alien4cloud.model.components.IndexedArtifactType;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.IndexedToscaElement;
import alien4cloud.model.components.PrimitiveIndexedDataType;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.repository.Repository;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.model.templates.TopologyTemplateVersion;
import alien4cloud.model.topology.Topology;
import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.model.PluginConfiguration;
import lombok.extern.slf4j.Slf4j;

/**
 * Elastic Search DAO for alien 4 cloud application.
 *
 * @author luc boutier
 */
@Slf4j
@Component("alien-es-dao")
public class ElasticSearchDAO extends ESGenericSearchDAO {

    public static final String TOSCA_ELEMENT_INDEX = "toscaelement";

    public static final String SUGGESTION_INDEX = "suggestion";

    /**
     * Initialize the dao after being loaded by spring (Create the indexes).
     */
    @PostConstruct
    public void initEnvironment() {
        // init ES annotation scanning
        try {
            getMappingBuilder().initialize("alien4cloud");
        } catch (IntrospectionException | IOException e) {
            throw new IndexingServiceException("Could not initialize elastic search mapping builder", e);
        }

        // init indices and mapped classes
        setJsonMapper(ElasticSearchMapper.getInstance());

        initIndices(TOSCA_ELEMENT_INDEX, null, IndexedCapabilityType.class, IndexedArtifactType.class, IndexedRelationshipType.class, IndexedNodeType.class,
                IndexedDataType.class, PrimitiveIndexedDataType.class);
        initIndices(TOSCA_ELEMENT_INDEX, null, IndexedArtifactToscaElement.class, IndexedToscaElement.class);

        initIndice(Application.class);
        initIndice(ApplicationVersion.class);
        initIndice(ApplicationEnvironment.class);
        initIndice(Topology.class);
        initIndice(Csar.class);
        initIndice(Repository.class);
        initIndice(Plugin.class);
        initIndice(PluginConfiguration.class);
        initIndice(TopologyTemplate.class);
        initIndice(TopologyTemplateVersion.class);
        initIndice(MetaPropConfiguration.class);

        initIndice(Orchestrator.class);
        initIndice(OrchestratorConfiguration.class);
        initIndice(Location.class);
        initIndice(LocationResourceTemplate.class);

        initIndice(Deployment.class);
        initIndice(CsarGitRepository.class);

        initIndice(DeploymentTopology.class);

        initIndices(SUGGESTION_INDEX, null, AbstractSuggestionEntry.class, SuggestionEntry.class, SimpleSuggestionEntry.class);

        initCompleted();
    }

    private void initIndice(Class<?> clazz) {
        initIndices(clazz.getSimpleName().toLowerCase(), null, clazz);
    }
}
