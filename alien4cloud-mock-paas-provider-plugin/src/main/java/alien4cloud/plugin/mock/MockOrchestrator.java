package alien4cloud.plugin.mock;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;

/**
 * Mock implementation for an orchestrator instance.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockOrchestrator extends MockPaaSProvider implements IOrchestratorPlugin<ProviderConfig> {
    @Inject
    private MockLocationConfigurerFactory mockLocationConfigurerFactory;

    @Override
    public ILocationConfiguratorPlugin getConfigurator(String locationType) {
        return mockLocationConfigurerFactory.newInstance(locationType);
    }
}