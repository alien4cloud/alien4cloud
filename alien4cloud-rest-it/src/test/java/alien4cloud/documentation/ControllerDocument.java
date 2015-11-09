package alien4cloud.documentation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;

import io.github.robwin.markup.builder.MarkupLanguage;
import io.github.robwin.swagger2markup.utils.ParameterUtils;
import io.github.robwin.swagger2markup.utils.PropertyUtils;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

/**
 * Generate doc for a given controller.
 */
@Slf4j
public class ControllerDocument extends MarkupDocument {
    public static final AtomicInteger INCREMENT = new AtomicInteger();
    private static final String PARAMETERS = "Parameters";
    private static final String RESPONSES = "Responses";
    private static final String EXAMPLE_CURL = "Example CURL request";
    private static final String EXAMPLE_REQUEST = "Example HTTP request";
    private static final String EXAMPLE_RESPONSE = "Example HTTP response";
    private static final String TYPE_COLUMN = "Type";
    private static final String HTTP_CODE_COLUMN = "HTTP Code";
    private static final String REQUEST_EXAMPLE_FILE_NAME = "http-request";
    private static final String RESPONSE_EXAMPLE_FILE_NAME = "http-response";
    private static final String CURL_EXAMPLE_FILE_NAME = "curl-request";
    private static final String PARAMETER = "Parameter";

    private boolean examplesEnabled;
    private String examplesFolderPath;
    private String category;

    private String controllerName;
    private AlienSwagger2MarkupConverter.Controller controller;

    public ControllerDocument(Swagger swagger, String category, Map.Entry<String, AlienSwagger2MarkupConverter.Controller> controllerEntry,
            MarkupLanguage markupLanguage, String examplesFolderPath) {
        super(swagger, markupLanguage);
        this.category = category;
        if (StringUtils.isNotBlank(examplesFolderPath)) {
            this.examplesEnabled = true;
            this.examplesFolderPath = examplesFolderPath;
        }
        this.controllerName = controllerEntry.getKey();
        this.controller = controllerEntry.getValue();
    }

    /**
     * Builds all paths of the Swagger model
     */
    @Override
    public MarkupDocument build() throws IOException {
        String parent = category == null ? "parent: [rest_api]" : "parent: [rest_api, rest_api_" + category + "]";
        this.markupDocBuilder.textLine("---");
        this.markupDocBuilder.textLine("layout: post");
        this.markupDocBuilder.textLine("title: " + controller.description);
        this.markupDocBuilder.textLine("root: ../../");
        this.markupDocBuilder.textLine("categories: DOCUMENTATION-1.1.0");
        this.markupDocBuilder.textLine(parent);
        this.markupDocBuilder.textLine("node_name: rest_api_controller_" + controllerName);
        this.markupDocBuilder.textLine("weight: " + INCREMENT.incrementAndGet());
        this.markupDocBuilder.textLine("---");
        this.markupDocBuilder.newLine();

        for (Map.Entry<String, Path> entry : controller.controllerPaths) {
            Path path = entry.getValue();
            if (path != null) {
                path("GET", entry.getKey(), path.getGet());
                path("PUT", entry.getKey(), path.getPut());
                path("DELETE", entry.getKey(), path.getDelete());
                path("POST", entry.getKey(), path.getPost());
                path("PATCH", entry.getKey(), path.getPatch());
            }
        }
        return this;
    }

    /**
     * Builds a path
     *
     * @param httpMethod the HTTP method of the path
     * @param resourcePath the URL of the path
     * @param operation the Swagger Operation
     */
    private void path(String httpMethod, String resourcePath, Operation operation) throws IOException {
        if (operation != null) {
            pathTitle(httpMethod, resourcePath, operation);
            descriptionSection(operation);
            parametersSection(operation);
            responsesSection(operation);
            consumesSection(operation);
            producesSection(operation);
            examplesSection(operation);
        }
    }

    private void pathTitle(String httpMethod, String resourcePath, Operation operation) {
        String summary = operation.getSummary();
        String title;
        if (StringUtils.isNotBlank(summary)) {
            title = summary;
            this.markupDocBuilder.sectionTitleLevel2(title);
            this.markupDocBuilder.listing(httpMethod + " " + resourcePath);
        } else {
            title = httpMethod + " " + resourcePath;
            this.markupDocBuilder.sectionTitleLevel2(title);
        }
    }

    private void descriptionSection(Operation operation) throws IOException {
        pathDescription(operation);
    }

    private void pathDescription(Operation operation) {
        String description = operation.getDescription();
        if (StringUtils.isNotBlank(description)) {
            this.markupDocBuilder.sectionTitleLevel3(DESCRIPTION);
            this.markupDocBuilder.paragraph(description);
        }
    }

    private void parametersSection(Operation operation) throws IOException {
        List<Parameter> parameters = operation.getParameters();
        if (CollectionUtils.isNotEmpty(parameters)) {
            List<String> headerAndContent = new ArrayList<>();
            // Table header row
            List<String> header = Arrays.asList(TYPE_COLUMN, NAME_COLUMN, DESCRIPTION_COLUMN, REQUIRED_COLUMN, SCHEMA_COLUMN, DEFAULT_COLUMN);
            headerAndContent.add(StringUtils.join(header, DELIMITER));
            for (Parameter parameter : parameters) {
                String type = ParameterUtils.getType(parameter, markupLanguage);
                String parameterType = WordUtils.capitalize(parameter.getIn() + PARAMETER);
                // Table content row
                List<String> content = Arrays.asList(parameterType, parameter.getName(), parameterDescription(operation, parameter),
                        Boolean.toString(parameter.getRequired()), type, ParameterUtils.getDefaultValue(parameter));
                headerAndContent.add(StringUtils.join(content, DELIMITER));
            }
            this.markupDocBuilder.sectionTitleLevel3(PARAMETERS);
            this.markupDocBuilder.newLine();
            this.markupDocBuilder.textLine("{: .table .table-bordered}");
            this.markupDocBuilder.tableWithHeaderRow(headerAndContent);
        }
    }

    private String parameterDescription(Operation operation, Parameter parameter) throws IOException {
        return StringUtils.defaultString(parameter.getDescription());
    }

    private void consumesSection(Operation operation) {
        List<String> consumes = operation.getConsumes();
        if (CollectionUtils.isNotEmpty(consumes)) {
            this.markupDocBuilder.sectionTitleLevel3(CONSUMES);
            this.markupDocBuilder.unorderedList(consumes);
        }

    }

    private void producesSection(Operation operation) {
        List<String> produces = operation.getProduces();
        if (CollectionUtils.isNotEmpty(produces)) {
            this.markupDocBuilder.sectionTitleLevel3(PRODUCES);
            this.markupDocBuilder.unorderedList(produces);
        }
    }

    /**
     * Builds the example section of a Swagger Operation
     *
     * @param operation the Swagger Operation
     * @throws IOException if the example file is not readable
     */
    private void examplesSection(Operation operation) throws IOException {
        if (examplesEnabled) {
            String summary = operation.getSummary();
            if (StringUtils.isNotBlank(summary)) {
                String exampleFolder = summary.replace(".", "").replace(" ", "_").toLowerCase();
                String curlExample = example(exampleFolder, CURL_EXAMPLE_FILE_NAME);
                if (StringUtils.isNotBlank(curlExample)) {
                    this.markupDocBuilder.sectionTitleLevel3(EXAMPLE_CURL);
                    this.markupDocBuilder.paragraph(curlExample);
                }

                String requestExample = example(exampleFolder, REQUEST_EXAMPLE_FILE_NAME);
                if (StringUtils.isNotBlank(requestExample)) {
                    this.markupDocBuilder.sectionTitleLevel3(EXAMPLE_REQUEST);
                    this.markupDocBuilder.paragraph(requestExample);
                }
                String responseExample = example(exampleFolder, RESPONSE_EXAMPLE_FILE_NAME);
                if (StringUtils.isNotBlank(responseExample)) {
                    this.markupDocBuilder.sectionTitleLevel3(EXAMPLE_RESPONSE);
                    this.markupDocBuilder.paragraph(responseExample);
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Example file cannot be read, because summary of operation is empty.");
                }
            }
        }
    }

    /**
     * Reads an example
     *
     * @param exampleFolder the name of the folder where the example file resides
     * @param exampleFileName the name of the example file
     * @return the content of the file
     * @throws IOException
     */
    private String example(String exampleFolder, String exampleFileName) throws IOException {
        for (String fileNameExtension : markupLanguage.getFileNameExtensions()) {
            java.nio.file.Path path = Paths.get(examplesFolderPath, exampleFolder, exampleFileName + fileNameExtension);
            if (Files.isReadable(path)) {
                if (log.isInfoEnabled()) {
                    log.info("Example file processed: {}", path);
                }
                return FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8.toString()).trim();
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Example file is not readable: {}", path);
                }
            }
        }
        if (log.isWarnEnabled()) {
            log.info("No example file found with correct file name extension in folder: {}", Paths.get(examplesFolderPath, exampleFolder));
        }
        return null;
    }

    private void responsesSection(Operation operation) {
        Map<String, Response> responses = operation.getResponses();
        if (MapUtils.isNotEmpty(responses)) {
            List<String> csvContent = new ArrayList<>();
            csvContent.add(HTTP_CODE_COLUMN + DELIMITER + DESCRIPTION_COLUMN + DELIMITER + SCHEMA_COLUMN);
            for (Map.Entry<String, Response> entry : responses.entrySet()) {
                Response response = entry.getValue();
                if (response.getSchema() != null) {
                    Property property = response.getSchema();
                    String type = PropertyUtils.getType(property, markupLanguage);
                    csvContent.add(entry.getKey() + DELIMITER + response.getDescription() + DELIMITER + type);
                } else {
                    csvContent.add(entry.getKey() + DELIMITER + response.getDescription() + DELIMITER + "No Content");
                }
            }
            this.markupDocBuilder.sectionTitleLevel3(RESPONSES);
            this.markupDocBuilder.newLine();
            this.markupDocBuilder.textLine("{: .table .table-bordered}");
            this.markupDocBuilder.tableWithHeaderRow(csvContent);
        }
    }
}