package alien4cloud;

import org.alien4cloud.bootstrap.Bootstrap;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;

/**
 * Entry point for war packaging.
 */
public class ServletInitializer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        Bootstrap.configure();
        return application.sources(Bootstrap.class);
    }
}