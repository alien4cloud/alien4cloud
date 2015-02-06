package alien4cloud.utils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Factory to create a {@link AlienYamlPropertiesFactoryBeanFactory} singleton.
 */
@Slf4j
public class AlienYamlPropertiesFactoryBeanFactory {
    public static final String ALIEN_CONFIGURATION = "alien4cloud-config";
    public static final String ALIEN_CONFIGURATION_YAML = ALIEN_CONFIGURATION + ".yml";

    private static final String[] SEARCH_LOCATIONS = new String[] { "file:./config/", "file:./", "classpath:/config/", "classpath:/" };
    private static YamlPropertiesFactoryBean INSTANCE;

    /**
     * Get a singleton instance of {@link YamlPropertiesFactoryBean}.
     * 
     * @param resourceLoader The loader to use to find the yaml file.
     * @return an instance of the {@link YamlPropertiesFactoryBean}.
     */
    public static YamlPropertiesFactoryBean get(ResourceLoader resourceLoader) {
        if (INSTANCE == null) {
            for (String searchLocation : SEARCH_LOCATIONS) {
                Resource resource = resourceLoader.getResource(searchLocation + ALIEN_CONFIGURATION_YAML);
                if (resource != null && resource.exists()) {
                    log.info("Loading Alien 4 Cloud configuration from {}", resource.getDescription());
                    INSTANCE = new YamlPropertiesFactoryBean();
                    INSTANCE.setResources(new Resource[] { resource });
                    return INSTANCE;
                }
            }
        }
        return INSTANCE;
    }
}