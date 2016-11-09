package alien4cloud.webconfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.ICsarAuthorizationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import alien4cloud.exception.InitializationException;
import alien4cloud.exception.NotFoundException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class StaticResourcesConfiguration extends WebMvcConfigurerAdapter {
    public final static String PLUGIN_STATIC_ENDPOINT = "/static/plugins/";

    @Setter
    private ICsarAuthorizationFilter csarAuthorizationFilter = null;
    @Setter
    private CsarService csarService = null;

    @Value("${directories.alien}/${directories.csar_repository}/")
    private String toscaRepo;
    @Value("${directories.alien}/work/plugins/ui/")
    private String pluginsUi;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = "file:///";
        // resource locations must be full path and not relatives
        String absToscaRepo = prefix.concat(safeGetRealPath(toscaRepo)).concat("/");
        String absPluginUi = prefix.concat(safeGetRealPath(pluginsUi)).concat("/");

        log.info("Serving {} as tosca repo content.", absToscaRepo);
        log.info("Serving {} as plugin ui content.", absPluginUi);

        registry.addResourceHandler("/static/tosca/{csarName:.+}/{csarVersion:.+}/**").addResourceLocations(absToscaRepo).resourceChain(false)
                .addResolver(new ResourceResolver() {
                    @Override
                    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations,
                            ResourceResolverChain chain) {
                        log.debug("Resolving editor resource");
                        // check security for the requested topology file.
                        ServletWebRequest webRequest = new ServletWebRequest(request);
                        Map uriTemplateVars = (Map) webRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 0);
                        String csarName = (String) uriTemplateVars.get("csarName");
                        String csarVersion = (String) uriTemplateVars.get("csarVersion");
                        if (csarAuthorizationFilter == null || csarService == null) {
                            // not initialized as master
                            throw new NotFoundException("Only master nodes can provide editor static resources.");
                        } else {
                            csarAuthorizationFilter.checkReadAccess(csarService.getOrFail(csarName, csarVersion));
                        }
                        // let the usual resolving
                        return chain.resolveResource(request, csarName + "/" + csarVersion + "/" + requestPath, locations);
                    }

                    @Override
                    public String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
                        return chain.resolveUrlPath(resourceUrlPath, locations);

                    }
                });
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