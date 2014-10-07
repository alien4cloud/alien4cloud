package alien4cloud.utils;

import java.util.Properties;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import alien4cloud.spring.config.YamlPropertiesFactoryBean;

/**
 * Initialize Alien application context legacy since we moved to spring boot.
 *
 * @author luc boutier
 */
public class Alien4CloudApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public static final String ALIEN_CONFIGURATION_PATH = "alien4cloud-config.yaml";
    private static final String LDAP_ACTIVATION_PROPERTY_KEY = "ldap.enabled";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // load alien configuration
        YamlPropertiesFactoryBean propertiesFactoryBean = new YamlPropertiesFactoryBean();
        propertiesFactoryBean.setResources(new Resource[] { new ClassPathResource(ALIEN_CONFIGURATION_PATH) });
        Properties properties = propertiesFactoryBean.getObject();

        boolean ldap = Boolean.parseBoolean((String) properties.get(LDAP_ACTIVATION_PROPERTY_KEY));
        String securityProfile = ldap ? "security-ldap" : "security-demo";

        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        environment.setActiveProfiles(securityProfile);
    }
}
