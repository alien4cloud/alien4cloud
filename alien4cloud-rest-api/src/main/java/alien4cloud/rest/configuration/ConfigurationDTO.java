package alien4cloud.rest.configuration;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Setter
@Getter
public class ConfigurationDTO {

    private String defaultLanguage;

    private String prefixLanguage;

    private Set supportedLanguages;

    private Map yolo;
}
