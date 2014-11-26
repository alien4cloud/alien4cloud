package alien4cloud.rest.form;

import java.beans.IntrospectionException;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.cloud.CloudService;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.plugin.PluginManager;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.ui.form.FormDescriptorGenerator;

import com.mangofactory.swagger.annotations.ApiIgnore;

@ApiIgnore
@Component
@RestController
@RequestMapping("/rest/formdescriptor")
public class FormDescriptorController {
    @Resource
    private FormDescriptorGenerator formDescriptorGenerator;

    @Resource
    private PluginManager pluginManager;
    @Resource
    private CloudService cloudService;

    @RequestMapping(value = "/nodetype", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Map<String, Object>> getNodeTypeFormDescriptor() throws IntrospectionException {
        return RestResponseBuilder.<Map<String, Object>> builder().data(formDescriptorGenerator.generateDescriptor(IndexedNodeType.class)).build();
    }

    @RequestMapping(value = "/tagconfiguration", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Map<String, Object>> getTagConfigurationFormDescriptor() throws IntrospectionException {
        return RestResponseBuilder.<Map<String, Object>> builder().data(formDescriptorGenerator.generateDescriptor(MetaPropConfiguration.class)).build();
    }

    @RequestMapping(value = "/pluginConfig/{pluginId:.+}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Map<String, Object>> getPluginConfigurationFormDescriptor(@PathVariable String pluginId) throws IntrospectionException {
        if (pluginManager.isPluginConfigurable(pluginId)) {
            Class<?> configType = pluginManager.getConfigurationType(pluginId);
            return RestResponseBuilder.<Map<String, Object>> builder().data(formDescriptorGenerator.generateDescriptor(configType)).build();
        }
        return RestResponseBuilder.<Map<String, Object>> builder().build();
    }

    @RequestMapping(value = "/cloudConfig/{cloudId:.+}", method = RequestMethod.GET, produces = "application/json")
    public RestResponse<Map<String, Object>> getCloudConfigurationFormDescriptor(@PathVariable String cloudId) throws IntrospectionException {
        Class<?> configurationClass = cloudService.getConfigurationType(cloudId);

        if (configurationClass != null) {
            return RestResponseBuilder.<Map<String, Object>> builder().data(formDescriptorGenerator.generateDescriptor(configurationClass)).build();
        }
        return RestResponseBuilder.<Map<String, Object>> builder().build();
    }
}
