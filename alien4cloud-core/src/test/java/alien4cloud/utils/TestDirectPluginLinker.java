package alien4cloud.utils;

import java.util.List;

import alien4cloud.plugin.IPluginLinker;
import alien4cloud.plugin.model.PluginUsage;

public class TestDirectPluginLinker implements IPluginLinker<String> {

    @Override
    public void link(String pluginId, String instanceId, String instance) {
    }

    @Override
    public void unlink(String pluginId) {
    }

    @Override
    public List<PluginUsage> usage(String pluginId) {
        return null;
    }
}