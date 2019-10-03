package alien4cloud.topology;

import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TopologyValidatorRegistry extends AbstractPluginLinker<ITopologyValidatorPlugin> {

    @Override
    public List<PluginUsage> usage(String pluginId) {
        return new ArrayList<>();
    }
}
