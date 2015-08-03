package alien4cloud.webconfiguration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class StaticResourcesConfiguration extends WebMvcConfigurerAdapter {
    @Value("file:///${directories.alien}/${directories.csar_repository}/")
    private String toscaRepo;
    @Value("file:///${directories.alien}/work/plugins/ui/")
    private String pluginsUi;

    public final static String PLUGIN_STATIC_ENDPOINT = "/static/plugins/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/tosca/**").addResourceLocations(toscaRepo);
        registry.addResourceHandler(PLUGIN_STATIC_ENDPOINT + "**").addResourceLocations(pluginsUi);
    }
}
