package alien4cloud;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;

import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

/**
 * The configuration to launch the full A4C context.
 */
@Configuration
@ComponentScan(basePackages = { "alien4cloud", "org.alien4cloud.tosca" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.webconfiguration\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.security\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.audit\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "org\\.elasticsearch\\.mapping\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.dao\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.servlet\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.images\\..*") })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FullApplicationConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(ResourceLoader resourceLoader) {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }

}
