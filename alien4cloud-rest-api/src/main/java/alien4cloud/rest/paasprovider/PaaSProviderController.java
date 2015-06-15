package alien4cloud.rest.paasprovider;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.IPaaSProviderFactory;
import alien4cloud.paas.PaaSProviderFactoriesService;
import alien4cloud.plugin.model.PluginComponentDescriptor;
import alien4cloud.plugin.model.PluginDescriptor;
import alien4cloud.plugin.PluginManager;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.ApiOperation;

/**
 * Controller for paas provider.
 */
@RestController
@RequestMapping("/rest/passprovider")
public class PaaSProviderController {
    @Resource
    private PaaSProviderFactoriesService paaSProviderFactoriesService;
    @Resource
    private PluginManager pluginManager;

    @ApiOperation(value = "Retrieve a list of available PaaSProvider .", notes = "")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<List<PaaSProviderDTO>> listPaaSProviders() {
        RestResponse<List<PaaSProviderDTO>> response = RestResponseBuilder.<List<PaaSProviderDTO>> builder().build();
        Map<String, Map<String, IPaaSProviderFactory>> instancesByPlugins = paaSProviderFactoriesService.getInstancesByPlugins();

        if (MapUtils.isEmpty(instancesByPlugins)) {
            return response;
        }

        List<PaaSProviderDTO> paasDeployers = Lists.newArrayList();
        for (Entry<String, Map<String, IPaaSProviderFactory>> entry : instancesByPlugins.entrySet()) {
            if (!MapUtils.isEmpty(entry.getValue())) {
                PluginDescriptor pluginDescriptor = pluginManager.getPluginDescriptor(entry.getKey());
                for (Entry<String, IPaaSProviderFactory> providerEntry : entry.getValue().entrySet()) {
                    PaaSProviderDTO providerDTO = new PaaSProviderDTO();
                    providerDTO.setPluginId(entry.getKey());
                    providerDTO.setPluginName(pluginDescriptor.getName());
                    providerDTO.setVersion(pluginDescriptor.getVersion());
                    providerDTO.setComponentDescriptor(getDescriptorFor(pluginDescriptor, providerEntry.getKey()));
                    paasDeployers.add(providerDTO);
                }
            }
        }

        response.setData(paasDeployers);

        return response;
    }

    private PluginComponentDescriptor getDescriptorFor(PluginDescriptor pluginDescriptor, String beanName) {
        PluginComponentDescriptor[] componentDescriptors = pluginDescriptor.getComponentDescriptors();
        if (!ArrayUtils.isEmpty(componentDescriptors)) {
            for (PluginComponentDescriptor pluginComponentDescriptor : componentDescriptors) {
                if (pluginComponentDescriptor.getBeanName().equals(beanName)) {
                    return pluginComponentDescriptor;
                }
            }
        }
        throw new NotFoundException("Component [" + beanName + "] cannot be found in plugin [" + pluginDescriptor.getId() + "]");
    }
}