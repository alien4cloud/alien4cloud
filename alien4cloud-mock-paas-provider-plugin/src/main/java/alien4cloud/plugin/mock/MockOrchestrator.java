package alien4cloud.plugin.mock;

import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mock implementation for an orchestrator instance.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockOrchestrator extends MockPaaSProvider implements IOrchestratorPlugin<ProviderConfig> {
    @Override
    public List<IndexedNodeType> getResourcesTypes() {
        return null;
    }

    @Override
    public List<IndexedNodeType> getTemplateTypes() {
        return null;
    }

    @Override
    public List<IndexedNodeType> getServiceTypes() {
        return null;
    }
}