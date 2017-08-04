package alien4cloud.webconfiguration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToscaContextFilterConfiguration {

    @Bean
    public FilterRegistrationBean toscaContextFilterRegistration(ServletRegistrationBean dispatcherRegistration) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(new ToscaContextFilter());
        return registrationBean;
    }
}