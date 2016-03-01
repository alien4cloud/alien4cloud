package alien4cloud.webconfiguration;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("!noApiDoc")
public class RestDocumentationConfig {

    private static final String CURRENT_API_VERSION = "1";
    private static final String PREFIXED_CURRENT_API_VERSION = "v" + CURRENT_API_VERSION;

    private List<Predicate<String>> predicates = Lists.newArrayList();

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
    public Docket componentsApiDocket() {
        Predicate predicate = Predicates.or(PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/components.*"),
                PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "csars.*"));
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("components-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket topologyTemplatesApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/templates.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("topology-templates-api").select().paths(predicate).build().apiInfo(apiInfo());
    }

    @Bean
    public Docket applicationApiDocket() {
        Predicate predicate = PathSelectors.regex("/rest/" + PREFIXED_CURRENT_API_VERSION + "/applications.*");
        predicates.add(predicate);
        return new Docket(DocumentationType.SWAGGER_2).groupName("applications-api").select().paths(predicate).build().apiInfo(apiInfo());
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
                "Join us on slack! http://localhost:4000/community/index.html", "Licensed under the Apache License, Version 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0");
        return apiInfo;
    }
}
