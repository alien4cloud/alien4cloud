package alien4cloud.rest.plugin;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.plugin.model.PluginComponentDescriptor;
import alien4cloud.plugin.model.PluginDescriptor;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.collect.Lists;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

/**
 * Created by lucboutier on 12/08/15.
 */
@RestController
@RequestMapping(value = {"/rest/plugincomponents", "/rest/v1/plugincomponents", "/rest/latest/plugincomponents"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Plugin Components", description = "Allow to query for enabled plugin components.", authorizations = { @Authorization("ADMIN") })
public class PluginComponentController {
    @Resource
    private PluginManager pluginManager;

    @ApiOperation(value = "Search for plugin components.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<PluginComponentDTO>> list(
            @ApiParam(value = "Type of plugin component to query for.", required = true) @RequestParam(required = true) String type) {
        RestResponse<List<PluginComponentDTO>> response = RestResponseBuilder.<List<PluginComponentDTO>> builder().build();

        Map<String, ManagedPlugin> managedPluginMap = pluginManager.getPluginContexts();
        List<PluginComponentDTO> result = Lists.newArrayList();

        for (ManagedPlugin plugin : managedPluginMap.values()) {
            PluginDescriptor descriptor = plugin.getPlugin().getDescriptor();
            for (PluginComponentDescriptor componentDescriptor : descriptor.getComponentDescriptors()) {
                if (componentDescriptor.getType().equals(type)) {
                    result.add(new PluginComponentDTO(plugin.getPlugin().getId(), descriptor.getName(), descriptor.getVersion(), componentDescriptor));
                }
            }
        }

        response.setData(result);

        return response;
    }
}
