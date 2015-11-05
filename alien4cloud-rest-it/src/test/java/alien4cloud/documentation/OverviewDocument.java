package alien4cloud.documentation;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;

import io.github.robwin.markup.builder.MarkupLanguage;
import io.swagger.models.Info;
import io.swagger.models.Swagger;

public class OverviewDocument extends MarkupDocument {
    public static final AtomicInteger INCREMENT = new AtomicInteger();

    private static final String OVERVIEW = "Overview";
    private static final String CURRENT_VERSION = "Version information";
    private static final String VERSION = "Version: ";

    private String category;

    public OverviewDocument(Swagger swagger, String category, MarkupLanguage markupLanguage) {
        super(swagger, markupLanguage);
        this.category = category;
    }

    /**
     * Builds the document header of the swagger model
     */
    @Override
    public MarkupDocument build() {
        String parent = category == null ? "parent: []" : "parent: [rest_api]";
        String node = category == null ? "node_name: rest_api" : "node_name: rest_api_" + category;
        String title = category == null ? "title: Rest API" : "title: " + category;
        String weight = category == null ? "weight: 9000" : "weight: " + INCREMENT.incrementAndGet();

        this.markupDocBuilder.textLine("---");
        this.markupDocBuilder.textLine("layout: post");
        this.markupDocBuilder.textLine(title);
        this.markupDocBuilder.textLine("root: ../../");
        this.markupDocBuilder.textLine("categories: DOCUMENTATION-1.1.0");
        this.markupDocBuilder.textLine(parent);
        this.markupDocBuilder.textLine(node);
        this.markupDocBuilder.textLine(weight);
        this.markupDocBuilder.textLine("---");
        this.markupDocBuilder.newLine();

        Info info = swagger.getInfo();
        this.markupDocBuilder.documentTitle(info.getTitle());
        this.markupDocBuilder.sectionTitleLevel1(OVERVIEW);
        if (StringUtils.isNotBlank(info.getDescription())) {
            this.markupDocBuilder.textLine("This section contains documentation of Alien 4 Cloud REST API.");
            this.markupDocBuilder.newLine();
        }
        if (StringUtils.isNotBlank(info.getVersion())) {
            this.markupDocBuilder.sectionTitleLevel2(CURRENT_VERSION);
            this.markupDocBuilder.textLine(VERSION + info.getVersion());
            this.markupDocBuilder.newLine();
        }

        return this;
    }
}
