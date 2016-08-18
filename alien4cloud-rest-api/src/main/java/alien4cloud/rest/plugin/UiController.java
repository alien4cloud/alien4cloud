package alien4cloud.rest.plugin;

import java.util.Map;

import javax.annotation.Resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.plugin.Plugin;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.webconfiguration.StaticResourcesConfiguration;

import com.google.common.collect.Maps;
import io.swagger.annotations.ApiOperation;
import springfox.documentation.annotations.ApiIgnore;

/**
 * UI Controller provide services used to manage the UI view including Angular Route management, User custom dashboards etc.
 */
@ApiIgnore
@RestController
@RequestMapping({"/rest/modules", "/rest/v1/modules", "/rest/latest/modules"})
public class UiController {
    @Resource
    private PluginManager pluginManager;

    private final String root = StaticResourcesConfiguration.PLUGIN_STATIC_ENDPOINT.substring(1);

    /**
     * Get the list of modules to be loaded.
     * 
     * @return The list of modules to be loaded.
     */
    @ApiOperation(value = "Get the list of ui modules to be loaded.")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, PluginInfo> modules() {
        Map<String, ManagedPlugin> plugins = pluginManager.getPluginContexts();
        Map<String, PluginInfo> entryPoints = Maps.newHashMap();
        for (ManagedPlugin managedPlugin : plugins.values()) {
            Plugin plugin = managedPlugin.getPlugin();
            String uiEntryPoint = plugin.getDescriptor().getUiEntryPoint();
            if (uiEntryPoint != null) {
                String pluginBase = root + plugin.getPluginPathId() + "/";
                String entryPoint = pluginBase + uiEntryPoint;
                entryPoints.put(managedPlugin.getPlugin().getDescriptor().getId(), new PluginInfo(entryPoint, pluginBase));
            }
        }
        return entryPoints;
    }

    @Getter
    @Setter
    @AllArgsConstructor(suppressConstructorProperties = true)
    public class PluginInfo {
        private String entryPoint;
        private String base;
    }
}