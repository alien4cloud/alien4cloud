package alien4cloud.webconfiguration;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.fasterxml.classmate.TypeResolver;

import alien4cloud.utils.AlienConstants;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.DocumentationPlugin;
import springfox.documentation.spi.service.contexts.Defaults;
import springfox.documentation.spi.service.contexts.DocumentationContext;
import springfox.documentation.spi.service.contexts.DocumentationContextBuilder;
import springfox.documentation.spi.service.contexts.Orderings;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.plugins.DefaultConfiguration;
import springfox.documentation.spring.web.plugins.DocumentationPluginsManager;
import springfox.documentation.spring.web.scanners.ApiDocumentationScanner;

/**
 * Override rest documentation bootstrapper from swagger so we can load plugins and .
 */
@Component
@Profile(AlienConstants.API_DOC_PROFILE_FILTER)
public class RestDocumentationPluginsBootstrapper implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(RestDocumentationPluginsBootstrapper.class);
    private final DocumentationPluginsManager documentationPluginsManager;
    private final RestDocumentationHandlerProvider handlerProvider;
    private final DocumentationCache scanned;
    private final ApiDocumentationScanner resourceListing;
    private final DefaultConfiguration defaultConfiguration;
    private AtomicBoolean initialized = new AtomicBoolean(false);

    @Autowired
    public RestDocumentationPluginsBootstrapper(DocumentationPluginsManager documentationPluginsManager, RestDocumentationHandlerProvider handlerProvider,
            DocumentationCache scanned, ApiDocumentationScanner resourceListing, TypeResolver typeResolver, Defaults defaults, ServletContext servletContext) {
        this.documentationPluginsManager = documentationPluginsManager;
        this.handlerProvider = handlerProvider;
        this.scanned = scanned;
        this.resourceListing = resourceListing;
        this.defaultConfiguration = new DefaultConfiguration(defaults, typeResolver, servletContext);
    }

    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (this.initialized.compareAndSet(false, true)) {
            refresh();
        }
    }

    /**
     * Refresh the documentation
     */
    public void refresh() {
        List plugins = Orderings.pluginOrdering().sortedCopy(this.documentationPluginsManager.documentationPlugins());
        logger.info("Found {} custom documentation plugin(s)", Integer.valueOf(plugins.size()));
        Iterator var3 = plugins.iterator();

        while (var3.hasNext()) {
            DocumentationPlugin each = (DocumentationPlugin) var3.next();
            DocumentationType documentationType = each.getDocumentationType();
            if (each.isEnabled()) {
                this.scanDocumentation(this.buildContext(each));
            } else {
                logger.info("Skipping initializing disabled plugin bean {} v{}", documentationType.getName(), documentationType.getVersion());
            }
        }
    }

    private DocumentationContext buildContext(DocumentationPlugin each) {
        return each.configure(this.defaultContextBuilder(each));
    }

    private void scanDocumentation(DocumentationContext context) {
        this.scanned.addDocumentation(this.resourceListing.scan(context));
    }

    private DocumentationContextBuilder defaultContextBuilder(DocumentationPlugin each) {
        DocumentationType documentationType = each.getDocumentationType();
        return this.documentationPluginsManager.createContextBuilder(documentationType, this.defaultConfiguration)
                .requestHandlers(this.handlerProvider.requestHandlers());
    }
}
