package alien4cloud.rest.configuration;

import alien4cloud.configuration.TranslationConfigurationService;
import alien4cloud.rest.model.*;
import alien4cloud.rest.utils.JsonUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * Get information about the configuration of A4C instances
 */
@Configuration
@RestController
@RequestMapping({"/rest/configuration", "/rest/v1/configuration", "/rest/latest/configuration"})
@Api
public class ConfigurationController {
    @Resource
    TranslationConfigurationService translationConfigurationService;

    @Value("${languages.default:en-us}")
    private String defaultLanguageFromConfig;
    @Value("${languages.prefix:locale}")
    private String prefixLanguageFromConfig;

    public ConfigurationController() {
    }

    @ApiOperation(value = "Get the UI configuration", notes = "Get the UI config of A4C.")
    @RequestMapping(method = RequestMethod.GET)
    public RestResponse<ConfigurationDTO> getDefault() {
        ConfigurationDTO configuration = new ConfigurationDTO();
        configuration.setDefaultLanguage(defaultLanguageFromConfig);
        configuration.setPrefixLanguage(prefixLanguageFromConfig);
        return RestResponseBuilder.<ConfigurationDTO> builder().data(configuration).build();
    }

    @ApiOperation(value = "List the supported languages", notes = "Returns a set with the available languages.")
    @RequestMapping(value="/supportedLanguages", method = RequestMethod.GET)
    public RestResponse<Map> getSupportedLanguages() {
        return RestResponseBuilder.<Map> builder().data(translationConfigurationService.getNameOfLanguagesCode()).build();
    }

    @Autowired
    private Environment environment;

    @Bean
    @ConfigurationProperties(prefix = "features.client")
    protected Properties getClientFeatures() {
        return new Properties();
    }

    @ApiOperation(value = "List the supported client features", notes = "Returns a set with available features.")
    @RequestMapping(value = "supportedFeatures", method = RequestMethod.GET)
    public RestResponse<Map<String,Object>> getSupportedFeatures()  {
        Map<String,Object> map = getClientFeatures().entrySet().stream().collect(Collectors.toMap( x -> (String) x.getKey(), x-> x.getValue()));
        return RestResponseBuilder.<Map<String,Object>> builder().data(map).build();
    }
}
