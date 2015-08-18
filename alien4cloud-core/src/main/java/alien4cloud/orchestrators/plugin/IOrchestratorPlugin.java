package alien4cloud.orchestrators.plugin;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IPaaSProvider;

import java.util.List;

/**
 * Interface used to communicate with an orchestrator.
 */
public interface IOrchestratorPlugin<T> extends IConfigurablePaaSProvider<T>, IPaaSProvider {
    List<IndexedNodeType> getResourcesTypes();

    List<IndexedNodeType> getTemplateTypes();

    /**
     * Services are running services provided by the orchestrator.
     *
     * @return
     */
    List<IndexedNodeType> getServiceTypes();
}