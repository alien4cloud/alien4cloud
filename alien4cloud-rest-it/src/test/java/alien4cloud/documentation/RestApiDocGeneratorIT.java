package alien4cloud.documentation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.robwin.swagger2markup.Swagger2MarkupConverter;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.apache.commons.lang3.Validate;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import springfox.documentation.staticdocs.Swagger2MarkupResultHandler;
import alien4cloud.rest.utils.RestClient;
import io.github.robwin.markup.builder.MarkupLanguage;

/**
 * Just package this as a rest it so documentation is generated when integration tests are processed.
 */
public class RestApiDocGeneratorIT {
    private RestClient restClient = new RestClient("http://localhost:8088/");

    @Test
    public void generateRestApiDoc() throws Exception {
        generateGroup("admin-user-api");
        generateGroup("admin-plugin-api");
        generateGroup("admin-orchestrator-api");
        generateGroup("admin-audit-api");
        generateGroup("admin-metaproperties-api");
        generateGroup("components-api");
        generateGroup("applications-api");
        generateGroup("topology-editor-api");
        generateGroup("other-apis");
        generateGroup("admin-api");

        String swaggerJson = restClient.get("v2/api-docs?group=admin-api");

        Validate.notEmpty(swaggerJson, "swagger must not be null!");
        ObjectMapper mapper;
        if (swaggerJson.trim().startsWith("{")) {
            mapper = Json.mapper();
        } else {
            mapper = Yaml.mapper();
        }
        JsonNode rootNode = mapper.readTree(swaggerJson);

        // must have swagger node set
        JsonNode swaggerNode = rootNode.get("swagger");
        if (swaggerNode == null)
            throw new IllegalArgumentException("Swagger String is in the wrong format");

        Swagger swagger = mapper.convertValue(rootNode, Swagger.class);

        new OverviewDocument(swagger, null, MarkupLanguage.MARKDOWN).build().writeToFile("target/docs", "overview", StandardCharsets.UTF_8);

    }

    private void generateGroup(String group) throws IOException {
        String swaggerJson = restClient.get("v2/api-docs?group=" + group);
        // .withExamples(this.examplesFolderPath)
        AlienSwagger2MarkupConverter.fromString(swaggerJson).withCategory(group).withMarkupLanguage(MarkupLanguage.MARKDOWN).build().intoFolder("target/docs");
    }
}