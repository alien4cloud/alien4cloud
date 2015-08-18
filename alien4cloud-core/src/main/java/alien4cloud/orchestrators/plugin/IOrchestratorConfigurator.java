package alien4cloud.orchestrators.plugin;

import java.util.List;

import alien4cloud.model.components.IndexedNodeType;

/**
 * Configurator for an orchestrator instance.
 */
public interface IOrchestratorConfigurator {
    /**
     * Provides the types allowed for Resource configurations.
     *
     * @return List of indexed Node
     */
    List<IndexedNodeType> types();
}
