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
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.management.MBeanServer;

/**
 * The configuration to launch the full A4C context.
 */
@Configuration
@ComponentScan(basePackages = { "alien4cloud", "org.alien4cloud" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.webconfiguration\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.security\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.audit\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "org\\.elasticsearch\\.mapping\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.dao\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.servlet\\..*"), @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.images\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "org\\.alien4cloud\\.bootstrap\\..*") })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FullApplicationConfiguration {

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(ResourceLoader resourceLoader) {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public AnnotationMBeanExporter mbeanExporter(ObjectNamingStrategy namingStrategy) {
        AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
        exporter.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);
        exporter.setNamingStrategy(namingStrategy);
//        String server = this.propertyResolver.getProperty("server", "mbeanServer");
//        if (StringUtils.hasLength(server)) {
//            exporter.setServer((MBeanServer)this.beanFactory.getBean(server, MBeanServer.class));
//        }

        return exporter;
    }
}
