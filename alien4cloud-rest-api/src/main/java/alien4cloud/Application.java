package alien4cloud;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import alien4cloud.servlet.ImageServlet;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" })
public class Application {
    private static final String ALIEN_CONFIGURATION = "alien4cloud-config";
    private static final String ALIEN_CONFIGURATION_YAML = ALIEN_CONFIGURATION + ".yml";

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
        System.setProperty("spring.config.name", ALIEN_CONFIGURATION);
    }

    @Bean(name = { "alienconfig", "elasticsearchConfig" })
    public static YamlPropertiesFactoryBean alienConfig() throws IOException {
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new Resource[] { new ClassPathResource(ALIEN_CONFIGURATION_YAML) });
        return yamlPropertiesFactoryBean;
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
