package alien4cloud;

import alien4cloud.servlet.ImageServlet;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.MultipartConfigFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.MultipartConfigElement;
import java.io.IOException;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "alien4cloud", "org.elasticsearch.mapping" })
public class Application {
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
}
