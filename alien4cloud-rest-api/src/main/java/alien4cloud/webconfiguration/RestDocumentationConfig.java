package alien4cloud.webconfiguration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.plugin.core.config.EnablePluginRegistries;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import alien4cloud.utils.AlienConstants;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.schema.configuration.ModelsConfiguration;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.PathDecorator;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ApiListingBuilderPlugin;
import springfox.documentation.spi.service.DefaultsProviderPlugin;
import springfox.documentation.spi.service.DocumentationPlugin;
import springfox.documentation.spi.service.ExpandedParameterBuilderPlugin;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.OperationModelsProviderPlugin;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.ResourceGroupingStrategy;
import springfox.documentation.spi.service.contexts.Defaults;
import springfox.documentation.spring.web.DocumentationCache;
import springfox.documentation.spring.web.ObjectMapperConfigurer;
import springfox.documentation.spring.web.json.JacksonModuleRegistrar;
import springfox.documentation.spring.web.json.JsonSerializer;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.spring.web.plugins.DocumentationPluginsBootstrapper;
import springfox.documentation.swagger.configuration.SwaggerCommonConfiguration;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.configuration.Swagger2JacksonModule;

@Configuration
@Import({ ModelsConfiguration.class, SwaggerCommonConfiguration.class })
@ComponentScan(basePackages = { "springfox.documentation.swagger2.readers.parameter", "springfox.documentation.swagger2.web",
        "springfox.documentation.swagger2.mappers", "springfox.documentation.spring.web.scanners", "springfox.documentation.spring.web.readers.operation",
        "springfox.documentation.spring.web.readers.parameter", "springfox.documentation.spring.web.plugins",
        "springfox.documentation.spring.web.paths" }, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DocumentationPluginsBootstrapper.class) })
@EnablePluginRegistries({ DocumentationPlugin.class, ApiListingBuilderPlugin.class, OperationBuilderPlugin.class, ParameterBuilderPlugin.class,
        ExpandedParameterBuilderPlugin.class, ResourceGroupingStrategy.class, OperationModelsProviderPlugin.class, DefaultsProviderPlugin.class,
        PathDecorator.class })
@Profile(AlienConstants.API_DOC_PROFILE_FILTER)
public class RestDocumentationConfig {

    private static final String CURRENT_API_VERSION = "1";
    private static final String PREFIXED_CURRENT_API_VERSION = "v" + CURRENT_API_VERSION;

    private List<Predicate<String>> predicates = Lists.newArrayList();

    @Bean
    public JacksonModuleRegistrar swagger2Module() {
        return new Swagger2JacksonModule();
    }

    @Bean
    public Defaults defaults() {
        return new Defaults();
    }

    @Bean
    public DocumentationCache resourceGroupCache() {
        return new DocumentationCache();
    }

    @Bean
    public static ObjectMapperConfigurer objectMapperConfigurer() {
        return new ObjectMapperConfigurer();
    }

    @Bean
    public JsonSerializer jsonSerializer(List<JacksonModuleRegistrar> moduleRegistrars) {
        return new JsonSerializer(moduleRegistrars);
    }

    @Bean
    public Docket adminApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/admin.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket adminUserApiDocket() {
        Predicate predicate = Predicates.or(PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/users.*"),
                PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/groups.*"));
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-user-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket adminPluginApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/plugins.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-plugin-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket adminOrchestratorApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/orchestrators.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-orchestrator-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket adminAuditApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/audit.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-audit-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket adminMetaPropertiesApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/metaproperties.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("admin-metaproperties-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket catalogApiDocket() {
        Predicate predicate = Predicates.or(PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/catalog.*"),
                PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/components.*"),
                PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/csars.*"));
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("catalog-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket workspacesApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/workspaces.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("workspaces-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket applicationApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/applications.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("applications-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket deploymentApiDocket() {
        Predicate predicate = Predicates.or(PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/deployments.*"),
                PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/runtime.*"));
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("applications-deployment-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket topologyEditorApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/topologies.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("topology-editor-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket otherApiDocket() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("other-apis").select().paths(otherApiPredicate()).build().apiInfo(apiInfo());
    }

    private Predicate<String> otherApiPredicate() {

        Predicate<String> notAlreadyTreated = Predicates.not(Predicates.or(predicates));
        Predicate<String> isCurrentVersionApi = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/.*");

        return Predicates.and(notAlreadyTreated, isCurrentVersionApi);
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo("ALIEN 4 Cloud API", "Welcome on the live configuration of ALIEN 4 Cloud Rest API.", CURRENT_API_VERSION, "",
                "Need help? Join us on slack! https://alien4cloud.github.io/community/index.html", "Licensed under the Apache License, Version 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0");
        return apiInfo;
    }

    @Bean
    public UiConfiguration uiConfig() {
        return new UiConfiguration(null);
    }
}
