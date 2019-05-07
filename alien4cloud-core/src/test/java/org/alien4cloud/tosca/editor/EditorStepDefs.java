package org.alien4cloud.tosca.editor;

import static org.alien4cloud.test.util.SPELUtils.evaluateAndAssertExpression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.ITopologyCatalogService;
import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.UpdateFileOperation;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.AbstractInstantiableToscaType;
import org.alien4cloud.tosca.model.types.AbstractToscaType;
import org.alien4cloud.tosca.model.types.ArtifactType;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.DataType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.PrimitiveDataType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.model.components.CSARSource;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.User;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.tosca.parser.ParserTestUtil;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUtil;
import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gherkin.formatter.model.DataTableRow;
import lombok.extern.slf4j.Slf4j;

@ContextConfiguration("classpath:application-context-test.xml")
@Slf4j
public class EditorStepDefs {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ArchiveUploadService csarUploadService;
    @Inject
    private EditorService editorService;
    @Inject
    private EditionContextManager editionContextManager;
    @Inject
    private CsarService csarService;
    @Inject
    private ITopologyCatalogService catalogService;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private EditorFileService editorFileService;

    private LinkedList<String> topologyIds = new LinkedList();

    private EvaluationContext topologyEvaluationContext;
    private EvaluationContext dtoEvaluationContext;
    private EvaluationContext exceptionEvaluationContext;
    private EvaluationContext csarEvaluationContext;

    private Exception thrownException;

    private Map<String, String> topologyIdToLastOperationId = new HashMap<>();

    private List<Class> typesToClean = Lists.newArrayList();
    public static final Path CSAR_TARGET_PATH = Paths.get("target/csars");

    public EditorStepDefs() {
        super();
        //typesToClean.add(AbstractInstantiableToscaType.class);
        //typesToClean.add(AbstractToscaType.class);
        typesToClean.add(CapabilityType.class);
        typesToClean.add(ArtifactType.class);
        typesToClean.add(RelationshipType.class);
        typesToClean.add(NodeType.class);
        typesToClean.add(DataType.class);
        typesToClean.add(PrimitiveDataType.class);
        typesToClean.add(Csar.class);
        typesToClean.add(Topology.class);
        typesToClean.add(Application.class);
        typesToClean.add(ApplicationEnvironment.class);
        typesToClean.add(ApplicationVersion.class);

    }

    @Before
    public void init() throws IOException {
        thrownException = null;

        GetMultipleDataResult<Application> apps = alienDAO.search(Application.class, "", null, 100);
        for (Application application : apps.getData()) {
            applicationService.delete(application.getId());
        }

        FacetedSearchResult<Topology> searchResult = catalogService.search(Topology.class, "", 100, null);
        Topology[] topologies = searchResult.getData();
        for (Topology topology : topologies) {
            try {
                csarService.forceDeleteCsar(topology.getId());
            } catch (NotFoundException e) {
                // Some previous tests may create topology without creating any archive, if so catch the exception
                alienDAO.delete(Topology.class, topology.getId());
            }
        }

        topologyIds.clear();
        editionContextManager.clearCache();
    }

    @Given("^I am authenticated with \"(.*?)\" role$")
    public void i_am_authenticated_with_role(String role) throws Throwable {
        User user = new User();
        user.setUsername("Username");
        user.setFirstName("firstName");
        user.setLastName("lastname");
        user.setEmail("user@fastco");
        Authentication auth = new TestAuth(user, role);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @When("^I get the archive with name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void i_Get_The_Archive_With_Name_And_Version(String name, String version) throws Throwable {
        Csar csar = csarService.get(name, version);
        csarEvaluationContext = new StandardEvaluationContext(csar);
    }

    @Then("^The csar SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void the_Csar_SPEL_Expression_Should_Return(String spelExpression, String expected) throws Throwable {
        evaluateAndAssertExpression(csarEvaluationContext, spelExpression, expected);
    }

    @And("^I delete the archive \"([^\"]*)\" \"([^\"]*)\" if any$")
    public void iDeleteTheArchiveIfAny(String name, String version) throws Throwable {
        try {
            csarService.deleteCsar(new Csar(name, version).getId());
        } catch (NotFoundException e) {
        }
    }

    @And("^I get the topology related to the CSAR with name \"([^\"]*)\" and version \"([^\"]*)\"$")
    public void iGetTheTopologyRelatedToTheCSARWithName(String archiveName, String archiveVersion) throws Throwable {
        Topology topology = catalogService.get(archiveName + ":" + archiveVersion);
        if (topology != null) {
            topologyIds.addLast(topology.getId());
        }
    }

    @And("^I should be able to find a component with id \"([^\"]*)\"$")
    public void iShouldBeAbleToFindAComponentWithId(String id) throws Throwable {
        AbstractToscaType compoennt = alienDAO.findById(AbstractToscaType.class, id);
        Assert.assertNotNull(compoennt);
    }

    @And("^I should not be able to find a component with id \"([^\"]*)\"$")
    public void iShouldNotBeAbleToFindAComponentWithId(String id) throws Throwable {
        Assert.assertNull(alienDAO.findById(AbstractToscaType.class, id));
    }

    private static class TestAuth extends UsernamePasswordAuthenticationToken {
        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        public TestAuth(User user, String role) {
            super(user, null);
            authorities.add(new SimpleGrantedAuthority(role));
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }
    }

    @Given("^I upload CSAR from path \"(.*?)\"$")
    public void i_upload_CSAR_from_path(String path) throws Throwable {
        uploadCsar(Paths.get(path));
    }

    @When("^I upload unzipped CSAR from path \"(.*?)\"$")
    public void i_upload_unzipped_CSAR_From_path(String path) throws Throwable {
        Path source = Paths.get(path);
        Path csarTargetPath = CSAR_TARGET_PATH.resolve(source.getFileName() + ".csar");
        FileUtil.zip(source, csarTargetPath);
        uploadCsar(csarTargetPath);
    }

    private void uploadCsar(Path path) throws Throwable {
        ParsingResult<Csar> result = csarUploadService.upload(path, CSARSource.UPLOAD, AlienConstants.GLOBAL_WORKSPACE_ID);
        if (result.getContext().getParsingErrors().size() > 0) {
            ParserTestUtil.displayErrors(result);
        }
        Assert.assertFalse(result.hasError(ParsingErrorLevel.ERROR));
    }

    @Given("^I cleanup archives$")
    public void i_cleanup_archives() throws Throwable {
        for (Class<?> type : typesToClean) {
            alienDAO.delete(type, QueryBuilders.matchAllQuery());
        }
    }

    @When("^I get the edited topology$")
    public void I_get_the_edited_topology() {
        thrownException = null;
        try {
            editionContextManager.init(topologyIds.getLast());
            Topology topology = editionContextManager.getTopology();
            topologyEvaluationContext = new StandardEvaluationContext(topology);
        } catch (Exception e) {
            log.error("Exception ocrured while getting the topology", e);
            thrownException = e;
            exceptionEvaluationContext = new StandardEvaluationContext(e);
        } finally {
            editionContextManager.destroy();
        }
    }

    @Given("^I create an empty topology$")
    public void i_create_an_empty_topology() throws Throwable {
        i_create_an_empty_topology_template(UUID.randomUUID().toString().replaceAll("-", "_"));
    }

    @Given("^I create an empty topology template \"([^\"]*)\"$")
    public void i_create_an_empty_topology_template(String topologyTemplateName) throws Throwable {
        i_create_an_empty_topology_template_version(topologyTemplateName, null);
    }

    @Given("^I create an empty topology template \"([^\"]*)\" version \"([^\"]*)\"$")
    public void i_create_an_empty_topology_template_version(String topologyTemplateName, String version) throws Throwable {
        try {
            Topology topology = catalogService.createTopologyAsTemplate(topologyTemplateName, null, version, AlienConstants.GLOBAL_WORKSPACE_ID, null);
            topologyIds.addLast(topology.getId());
            topologyEvaluationContext = new StandardEvaluationContext(topology);
        } catch (Exception e) {
            log.error("Exception occurred while creating a topology template", e);
            thrownException = e;
            exceptionEvaluationContext = new StandardEvaluationContext(e);
        }
    }

    @Given("^I execute the operation on the topology number (\\d+)$")
    public void i_execute_the_operation_on_topology_number(int indexOfTopologyId, DataTable operationDT) throws Throwable {
        Map<String, String> operationMap = Maps.newHashMap();
        for (DataTableRow row : operationDT.getGherkinRows()) {
            operationMap.put(row.getCells().get(0), row.getCells().get(1));
        }

        Class operationClass = Class.forName(operationMap.get("type"));
        AbstractEditorOperation operation = (AbstractEditorOperation) operationClass.newInstance();
        EvaluationContext operationContext = new StandardEvaluationContext(operation);
        SpelParserConfiguration config = new SpelParserConfiguration(true, true);
        SpelExpressionParser parser = new SpelExpressionParser(config);
        for (Map.Entry<String, String> operationEntry : operationMap.entrySet()) {
            if (!"type".equals(operationEntry.getKey())) {
                parser.parseRaw(operationEntry.getKey()).setValue(operationContext, getValue(operationEntry.getValue()));
            }
        }
        doExecuteOperation(operation, topologyIds.get(indexOfTopologyId));
    }

    private Object getValue(String rawValue) throws Throwable {
        String value = rawValue.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            return JsonUtil.toMap(value);
        }
        return value;
    }

    @Given("^I execute the operation$")
    public void i_execute_the_operation(DataTable operationDT) throws Throwable {
        i_execute_the_operation_on_topology_number(topologyIds.size() - 1, operationDT);
    }

    @Given("^I save the topology$")
    public void i_save_the_topology() throws Throwable {
        editorService.save(topologyIds.getLast(), topologyIdToLastOperationId.get(topologyIds.getLast()));
        topologyIdToLastOperationId.put(topologyIds.getLast(), null);
    }

    @Given("^I upload a file located at \"(.*?)\" to the archive path \"(.*?)\"$")
    public void i_upload_a_file_located_at_to_the_archive_path(String filePath, String archiveTargetPath) throws Throwable {
        UpdateFileOperation updateFileOperation = new UpdateFileOperation(archiveTargetPath, Files.newInputStream(Paths.get(filePath)));
        doExecuteOperation(updateFileOperation);
    }

    @Given("^I load variables for \"(.*?)\" / \"(.*?)\"$")
    public void i_load_variables_for(String scopeType, String scopeId) throws Throwable {
        Map<String, Object> variables;
        switch (scopeType) {
        case "ENV":
            variables = editorFileService.loadEnvironmentVariables(topologyIds.getLast(), scopeId);
            break;
        case "EVN_TYPE":
            variables = editorFileService.loadEnvironmentTypeVariables(topologyIds.getLast(), EnvironmentType.valueOf(scopeId));
            break;
        default:
            throw new UnsupportedOperationException("Not supported scope type for checking: " + scopeType);
        }

        topologyEvaluationContext = new StandardEvaluationContext(variables);
    }

    @When("^I load preconfigured inputs$")
    public void i_load_preconfigured_inputs() throws Throwable {
        topologyEvaluationContext = new StandardEvaluationContext(editorFileService.loadInputsVariables(topologyIds.getLast()));
    }

    private void doExecuteOperation(AbstractEditorOperation operation, String topologyId) {
        thrownException = null;
        operation.setPreviousOperationId(topologyIdToLastOperationId.get(topologyId));
        try {
            TopologyDTO topologyDTO = editorService.execute(topologyId, operation);
            String lastOperationId = topologyDTO.getOperations().get(topologyDTO.getLastOperationIndex()).getId();
            topologyIdToLastOperationId.put(topologyId, lastOperationId);
            topologyEvaluationContext = new StandardEvaluationContext(topologyDTO.getTopology());
            dtoEvaluationContext = new StandardEvaluationContext(topologyDTO);
        } catch (Exception e) {
            log.error("Exception occurred while executing operation", e);
            thrownException = e;
            exceptionEvaluationContext = new StandardEvaluationContext(e);
        }
    }

    private void doExecuteOperation(AbstractEditorOperation operation) {
        doExecuteOperation(operation, topologyIds.getLast());
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentTopologyContext(String spelExpression, String expected) {
        evaluateAndAssertExpression(topologyEvaluationContext, spelExpression, expected);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return (true|false)$")
    public void evaluateSpelExpressionUsingCurrentTopologyContext(String spelExpression, Boolean expected) {
        evaluateAndAssertExpression(topologyEvaluationContext, spelExpression, expected);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return (\\d+)$")
    public void evaluateSpelExpressionUsingCurrentTopologyContext(String spelExpression, Integer expected) {
        evaluateAndAssertExpression(topologyEvaluationContext, spelExpression, expected);
    }

    @Then("^The dto SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentDTOContext(String spelExpression, String expected) {
        evaluateAndAssertExpression(dtoEvaluationContext, spelExpression, expected);
    }

    @Then("^The dto SPEL expression \"([^\"]*)\" should return (true|false)$")
    public void evaluateSpelExpressionUsingCurrentDTOContext(String spelExpression, Boolean expected) {
        evaluateAndAssertExpression(dtoEvaluationContext, spelExpression, expected);
    }

    @Then("^The dto SPEL expression \"([^\"]*)\" should return (\\d+)$")
    public void evaluateSpelExpressionUsingCurrentDTOContext(String spelExpression, Integer expected) {
        evaluateAndAssertExpression(dtoEvaluationContext, spelExpression, expected);
    }

    @Then("^The exception SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentExceptionContext(String spelExpression, String expected) {
        evaluateAndAssertExpression(exceptionEvaluationContext, spelExpression, expected);
    }

    @Then("^The exception SPEL expression \"([^\"]*)\" should return (true|false)$")
    public void evaluateSpelExpressionUsingCurrentExceptionContext(String spelExpression, Boolean expected) {
        evaluateAndAssertExpression(exceptionEvaluationContext, spelExpression, expected);
    }

    @Then("^The exception SPEL expression \"([^\"]*)\" should return (\\d+)$")
    public void evaluateSpelExpressionUsingCurrentExceptionContext(String spelExpression, Integer expected) {
        evaluateAndAssertExpression(exceptionEvaluationContext, spelExpression, expected);
    }

    @Then("^No exception should be thrown$")
    public void no_exception_should_be_thrown() throws Throwable {
        Assert.assertNull(thrownException);
    }

    @Then("^an exception of type \"(.*?)\" should be thrown$")
    public void an_exception_of_type_should_be_thrown(String exceptionTypesStr) throws Throwable {
        String[] exceptionTypes = exceptionTypesStr.split("/");
        Throwable checkException = thrownException;
        for (String exceptionType : exceptionTypes) {
            Class<?> exceptionClass = Class.forName(exceptionType);
            Assert.assertNotNull(checkException);
            Assert.assertEquals(exceptionClass, checkException.getClass());
            checkException = checkException.getCause();
        }
    }

    @When("^I recover the topology$")
    public void i_Recover_The_Topology() throws Throwable {
        thrownException = null;
        try {
            TopologyDTO dto = editorService.recover(topologyIds.getLast(), topologyIdToLastOperationId.get(topologyIds.getLast()));
            topologyIdToLastOperationId.put(topologyIds.getLast(), null);
            dtoEvaluationContext = new StandardEvaluationContext(dto);
            topologyEvaluationContext = new StandardEvaluationContext(dto.getTopology());
        } catch (Exception e) {
            log.error("Error occurred when recovering the topology", e);
            thrownException = e;
            exceptionEvaluationContext = new StandardEvaluationContext(e);
        }
    }

    @When("^I reset the topology$")
    public void iResetTheTopology() throws Throwable {
        thrownException = null;
        try {
            TopologyDTO dto = editorService.reset(topologyIds.getLast(), topologyIdToLastOperationId.get(topologyIds.getLast()));
            topologyIdToLastOperationId.put(topologyIds.getLast(), null);
            dtoEvaluationContext = new StandardEvaluationContext(dto);
            topologyEvaluationContext = new StandardEvaluationContext(dto.getTopology());
        } catch (Exception e) {
            log.error("Error occurred when resetting the topology", e);
            thrownException = e;
            exceptionEvaluationContext = new StandardEvaluationContext(e);
        }
    }
}
