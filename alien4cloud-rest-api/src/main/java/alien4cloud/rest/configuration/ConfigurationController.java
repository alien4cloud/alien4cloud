package alien4cloud.rest.configuration;

import alien4cloud.configuration.TranslationConfigurationService;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;


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
}
