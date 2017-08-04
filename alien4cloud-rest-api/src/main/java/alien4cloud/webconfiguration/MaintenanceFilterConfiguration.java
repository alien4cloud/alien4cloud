package alien4cloud.webconfiguration;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Lists;

@Configuration
public class MaintenanceFilterConfiguration {

    @Bean
    public MaintenanceFilter maintenanceFilter() {
        return new MaintenanceFilter();
    }

    @Bean
    public FilterRegistrationBean maintenanceFilterRegistration(ServletRegistrationBean dispatcherRegistration, MaintenanceFilter maintenanceFilter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(maintenanceFilter);
        registrationBean.setOrder(1);
        registrationBean.setUrlPatterns(Lists.newArrayList("/rest/*"));
        return registrationBean;
    }
}