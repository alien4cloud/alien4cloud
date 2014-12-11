package alien4cloud.test;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.MultipartConfigElement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.yaml.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import alien4cloud.servlet.ImageServlet;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" })
public class Application {
    private static final String CONFIG_FILE = "file:./src/test/resources/alien4cloud-config.yml";

	/**
     * Alien 4 cloud standalone entry point.
     *
     * @param args Arguments that will be delegated to spring.
     */
    public static void main(String[] args) {
        configure();
        SpringApplication.run(Application.class, args);
    }

    public static void configure() {
        System.setProperty("security.basic.enabled", "false");
        System.setProperty("management.contextPath", "/rest/admin");
        System.setProperty("spring.config.name", AlienYamlPropertiesFactoryBeanFactory.ALIEN_CONFIGURATION);
    }

	@Bean(name = { "alienconfig", "elasticsearchConfig" })
	public YamlPropertiesFactoryBean alienConfig(ResourceLoader resourceLoader)
			throws IOException {
		Resource resource = resourceLoader.getResource(CONFIG_FILE);
		if (resource != null && resource.exists()) {
			YamlPropertiesFactoryBean config = new YamlPropertiesFactoryBean();
			config.setResources(new Resource[] { resource });
			return config;
		} else {
			throw new FileNotFoundException(CONFIG_FILE + " is not found to instance UI test");
		}
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
}
