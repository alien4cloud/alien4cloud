package alien4cloud.webconfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("!noApiDoc")
public class RestDocumentationConfig {
    @Bean
    public Docket swaggerSpringMvcPlugin() {
        return new Docket(DocumentationType.SWAGGER_2).groupName("business-api").select().build().apiInfo(apiInfo());
    }

    private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo("ALIEN 4 Cloud API", "Welcome on the live configuration of ALIEN 4 Cloud Rest API.", "1.1.0", "",
                "alien-support@fastconnect.fr", "Licensed under the Apache License, Version 2.0", "http://www.apache.org/licenses/LICENSE-2.0");
        return apiInfo;
    }
}