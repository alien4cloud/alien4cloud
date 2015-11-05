package alien4cloud.documentation;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import io.github.robwin.markup.builder.MarkupDocBuilders;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import io.github.robwin.markup.builder.MarkupDocBuilder;
import io.github.robwin.markup.builder.MarkupLanguage;
import io.swagger.models.*;

public class OverviewDocument extends MarkupDocument {

    private static final String OVERVIEW = "Overview";
    private static final String CURRENT_VERSION = "Version information";
    private static final String VERSION = "Version: ";

    public OverviewDocument(Swagger swagger, MarkupLanguage markupLanguage) {
        super(swagger, markupLanguage);
    }

    /**
     * Builds the document header of the swagger model
     */
    @Override
    public MarkupDocument build() {
        this.markupDocBuilder.textLine("---");
        this.markupDocBuilder.textLine("layout: post");
        this.markupDocBuilder.textLine("title: Rest API");
        this.markupDocBuilder.textLine("root: ../../");
        this.markupDocBuilder.textLine("categories: DOCUMENTATION-1.1.0");
        this.markupDocBuilder.textLine("parent: []");
        this.markupDocBuilder.textLine("node_name: rest_api");
        this.markupDocBuilder.textLine("weight: 9000");
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
