package org.alien4cloud.secret;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.secret.services.SecretProviderService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.ui.form.PojoFormDescriptorGenerator;
import io.swagger.annotations.Api;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping(value = { "/rest/secret", "/rest/v1/secret", "/rest/latest/secret" })
@Api(value = "", description = "Operations to manage secrets in Alien4Cloud")
public class SecretController {

    @Resource
    private SecretProviderService secretProviderService;

    @Resource
    private PojoFormDescriptorGenerator pojoFormDescriptorGenerator;

    /**
     * Retrieve all plugin available secret provider plugins in the system.
     * 
     * @return the response which contains list of available plugins' names in the system.
     */
    @ApiIgnore
    @RequestMapping(value = "/plugin-names", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Set<String>> getAvailablePlugins() {
        return RestResponseBuilder.<Set<String>> builder().data(secretProviderService.getAvailablePlugins()).build();
    }

    /**
     * Retrieve a specific plugin configuration description.
     * 
     * @param pluginName the name of the plugin
     * @return the response which contains the generic form descriptor for the plugin configuration
     */
    @ApiIgnore
    @RequestMapping(value = "/plugins/{pluginName}/configuration", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Map<String, Object>> getPluginConfigurationDescription(@PathVariable("pluginName") String pluginName) {
        return RestResponseBuilder.<Map<String, Object>> builder()
                .data(pojoFormDescriptorGenerator.generateDescriptor(secretProviderService.getPluginConfigurationDescriptor(pluginName))).build();
    }

    @ApiIgnore
    @RequestMapping(value = "/plugins/location/{locationId}", method = RequestMethod.GET)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Map<String, Object>> getPluginConfigurationDescription2(@PathVariable("pluginName") String pluginName) {
        // tout les plugins decsription
        // + data
        return RestResponseBuilder.<Map<String, Object>> builder()
                .data(pojoFormDescriptorGenerator.generateDescriptor(secretProviderService.getPluginConfigurationDescriptor(pluginName))).build();
    }


    /**
     * Retrieve a specific plugin configuration description.
     *
     * @param pluginName the name of the plugin
     * @return the response which contains the generic form descriptor for the plugin configuration
     */
    @ApiIgnore
    @RequestMapping(value = "/plugins/{pluginName}/authentication-configuration", consumes = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @PreAuthorize("isAuthenticated()")
    public RestResponse<Map<String, Object>> getPluginConfigurationDescription(@PathVariable("pluginName") String pluginName,
            @RequestBody Object pluginConfiguration) {
        return RestResponseBuilder.<Map<String, Object>> builder().data(pojoFormDescriptorGenerator
                .generateDescriptor(secretProviderService.getPluginAuthenticationConfigurationDescriptor(pluginName, pluginConfiguration))).build();
    }
}
