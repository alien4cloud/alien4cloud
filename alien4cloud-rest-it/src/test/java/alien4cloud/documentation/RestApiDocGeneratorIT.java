package alien4cloud.documentation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import io.github.robwin.swagger2markup.Swagger2MarkupConverter;
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
    @Test
    public void generateRestApiDoc() throws Exception {
        RestClient restClient = new RestClient("http://localhost:8088/");
        String swaggerJson = restClient.get("v2/api-docs?group=business-api");
        // .withExamples(this.examplesFolderPath)
        AlienSwagger2MarkupConverter.fromString(swaggerJson).withMarkupLanguage(MarkupLanguage.MARKDOWN).build().intoFolder("target/docs");
        // Swagger2MarkupConverter.fromString(swaggerJson).withMarkupLanguage(MarkupLanguage.MARKDOWN).build().intoFolder("target/docs");
    }
}