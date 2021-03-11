package alien4cloud.webconfiguration;

import alien4cloud.rest.wizard.model.WizardAddon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.web.servlet.ViewResolver;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.annotation.Resource;

/**
 * The Thymeleaf templating engine is just used to manager wizard and addons html pages.
 */
@Configuration
public class WebTemplatingConfiguration {

    @Resource
    private WizardAddonsScanner wizardAddonsScanner;

    private ClassLoaderTemplateResolver buildClassLoaderTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setCacheable(false);
        // LEGACYHTML5 since webpack generated malformated html
        templateResolver.setTemplateMode("LEGACYHTML5");
        templateResolver.setCharacterEncoding("UTF-8");
        return templateResolver;
    }

    @Bean
    @Description("Thymeleaf template resolver serving HTML 5 for wizard")
    public ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver resolver = buildClassLoaderTemplateResolver();
        resolver.setPrefix("wizard/");
        resolver.setSuffix(".html");
        resolver.setName("Wizard resolver");
        return resolver;
    }

    @Bean
    @Description("Thymeleaf template engine with Spring integration")
    public SpringTemplateEngine templateEngine() {

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(templateResolver());

        // Add template resolvers for wizard addons
        if (!wizardAddonsScanner.getAddons().isEmpty()) {
            ClassLoaderTemplateResolver resolver = buildClassLoaderTemplateResolver();
            resolver.setName("Wizard addons resolver");
            for (WizardAddon addon : wizardAddonsScanner.getAddons().values()) {
                // We use alias so we can name the view using the contextPath, even if all addon have the same addon.html
                resolver.addTemplateAlias(addon.getContextPath(), addon.getContextPath() + "/wizard_addon.html");
            }
            templateEngine.addTemplateResolver(resolver);
        }

        return templateEngine;
    }

    @Bean
    @Description("Thymeleaf view resolver")
    public ViewResolver viewResolver() {

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();

        viewResolver.setTemplateEngine(templateEngine());
        viewResolver.setCharacterEncoding("UTF-8");

        return viewResolver;
    }

}
