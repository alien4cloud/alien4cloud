package alien4cloud.webconfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import alien4cloud.exception.InitializationException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Slf4j
@Configuration
public class StaticResourcesConfiguration extends WebMvcConfigurerAdapter {
    @Value("${directories.alien}/${directories.csar_repository}/")
    private String toscaRepo;
    @Value("${directories.alien}/work/plugins/ui/")
    private String pluginsUi;

    public final static String PLUGIN_STATIC_ENDPOINT = "/static/plugins/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = "file:///";
        // resource locations must be full path and not relatives
        String absToscaRepo = prefix.concat(safeGetRealPath(toscaRepo)).concat("/");
        String absPluginUi = prefix.concat(safeGetRealPath(pluginsUi)).concat("/");
        log.info("Serving {} as tosca repo content.", absToscaRepo);
        log.info("Serving {} as plugin ui content.", absPluginUi);
        registry.addResourceHandler("/static/tosca/**").addResourceLocations(absToscaRepo);
        registry.addResourceHandler(PLUGIN_STATIC_ENDPOINT + "**").addResourceLocations(absPluginUi);
    }

    private String safeGetRealPath(String pathStr) {
        Path path = Paths.get(pathStr);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return path.toRealPath().toString();
        } catch (IOException e) {
            throw new InitializationException("Unable to initialize alien as specified directories cannot be created.", e);
        }
    }
}