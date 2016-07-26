package alien4cloud.plugin.mock;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import alien4cloud.plugin.IPluginConfigurator;
import alien4cloud.rest.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("mock-paas-provider-configurator")
public class ProviderConfigurator implements IPluginConfigurator<ProviderConfig> {

    @Override
    public ProviderConfig getDefaultConfiguration() {
        return new ProviderConfig();
    }

    @Override
    public void setConfiguration(ProviderConfig configuration) {
        log.info("In the plugin configurator <" + this.getClass().getName() + ">");
        try {
            log.info("The config object Tags is: " + JsonUtil.toString(configuration.getTags()));
        } catch (JsonProcessingException e) {
            log.error("Fails to serialize configuration object as json string", e);
        }
    }
}