package alien4cloud.orchestrators.plugin;

import alien4cloud.model.components.IndexedNodeType;

import java.util.List;

/**
 * Interface used to communicate with an orchestrator.
 */
public interface IOrchestrator<T> {
    List<IndexedNodeType> getResourcesTypes();

    List<IndexedNodeType> getTemplateTypes();

    /**
     * Services are running services provided by the orchestrator.
     *
     * @return
     */
    List<IndexedNodeType> getServiceTypes();
}