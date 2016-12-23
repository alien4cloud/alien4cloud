package org.alien4cloud.bootstrap;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import alien4cloud.servlet.ImageServlet;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

@Configuration("bootstrap-config")
@EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = { "org.alien4cloud.bootstrap", "alien4cloud.audit", "alien4cloud.security", "alien4cloud.webconfiguration",
        "org.alien4cloud.exception", "org.elasticsearch.mapping", "alien4cloud.dao", "alien4cloud.servlet", "alien4cloud.images" })
public class Bootstrap {

    /**
     * Alien 4 cloud standalone entry point.
     *
     * @param args Arguments that will be delegated to spring.
     */
    public static void main(String[] args) {
        configure();
        SpringApplication.run(Bootstrap.class, args);
    }

    public static void configure() {
        System.setProperty("security.basic.enabled", "false");
        System.setProperty("management.contextPath", "/rest/admin");
        System.setProperty("spring.config.name", AlienYamlPropertiesFactoryBeanFactory.ALIEN_CONFIGURATION);
    }

    @Bean(name = { "alienconfig", "elasticsearchConfig" })
    public static YamlPropertiesFactoryBean alienConfig(ResourceLoader resourceLoader) throws IOException {
        return AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
    }

    @Bean
    public MultipartConfigElement multipartConfigElement(@Value("${upload.max_archive_size}") long maxUploadSize) {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxRequestSize(maxUploadSize);
        factory.setMaxFileSize(maxUploadSize);
        return factory.createMultipartConfig();
    }

    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    @Bean
    public ServletRegistrationBean dispatcherRegistration(DispatcherServlet dispatcherServlet, MultipartConfigElement multipartConfig) {
        ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet);
        registration.addUrlMappings("/*");
        registration.setMultipartConfig(multipartConfig);
        registration.setAsyncSupported(true);
        return registration;
    }

    @Bean
    public ServletRegistrationBean imageRegistration(ImageServlet imageServlet) {
        ServletRegistrationBean registration = new ServletRegistrationBean(imageServlet);
        registration.addUrlMappings("/img/*");
        return registration;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeHolderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        // this to avoid errors when properties are not resolvable
        // beans should use default value syntax to verify that the property exists
        // @Value("${some.key:#{null}}")
        configurer.setIgnoreUnresolvablePlaceholders(true);
        return configurer;
    }
}
