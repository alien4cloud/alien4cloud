package alien4cloud.rest.plugin;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.PluginComponent;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

@RestController
@RequestMapping(value = { "/rest/plugincomponents", "/rest/v1/plugincomponents", "/rest/latest/plugincomponents" }, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "Plugin Components", description = "Allow to query for enabled plugin components.", authorizations = { @Authorization("ADMIN") })
public class PluginComponentController {
    @Resource
    private PluginManager pluginManager;

    @ApiOperation(value = "Search for repository plugin components.")
    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ADMIN')")
    public RestResponse<List<PluginComponent>> list(@ApiParam(value = "Type of plugin component to query for.", required = true) @RequestParam String type) {
        return RestResponseBuilder.<List<PluginComponent>> builder().data(pluginManager.getPluginComponents(type)).build();
    }
}
