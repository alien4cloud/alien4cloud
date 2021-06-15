package alien4cloud.suggestions.services;

import alien4cloud.plugin.AbstractPluginLinker;
import alien4cloud.plugin.model.PluginUsage;
import alien4cloud.suggestions.ISuggestionPluginProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SuggestionPluginRegistry extends AbstractPluginLinker<ISuggestionPluginProvider> {

    @Override
    public List<PluginUsage> usage(String pluginId) {
        return new ArrayList<>();
    }
}
