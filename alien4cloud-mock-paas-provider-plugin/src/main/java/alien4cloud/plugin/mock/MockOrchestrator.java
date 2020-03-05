package alien4cloud.plugin.mock;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.model.PluginArchive;

import com.google.common.collect.Lists;

/**
 * Mock implementation for an orchestrator instance.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockOrchestrator extends MockPaaSProvider {
    @Override
    public List<PluginArchive> pluginArchives() {
        return Lists.newArrayList();
    }
}