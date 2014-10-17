package alien4cloud.documentation;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {
    @Bean
    public FilterRegistrationBean metricsFilterRegistration() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(new CacheFilter());
        registrationBean.addUrlPatterns("/img");
        return registrationBean;
    }
}