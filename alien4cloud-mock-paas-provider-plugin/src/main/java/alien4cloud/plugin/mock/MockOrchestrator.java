package alien4cloud.plugin.mock;

import alien4cloud.orchestrators.plugin.IOrchestrator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Mock implementation for an orchestrator instance.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockOrchestrator implements IOrchestrator<ProviderConfig> {

}