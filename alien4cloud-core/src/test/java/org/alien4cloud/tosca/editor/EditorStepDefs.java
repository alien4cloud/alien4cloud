package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.AddNodeOperation;
import org.alien4cloud.tosca.editor.operations.nodetemplate.DeleteNodeOperation;
import org.alien4cloud.tosca.editor.operations.relationshiptemplate.AddRelationshipOperation;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.components.*;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.wf.WorkflowsBuilderService;
import alien4cloud.security.model.User;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.ArchiveUploadService;
import cucumber.api.DataTable;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import gherkin.formatter.model.DataTableRow;
import lombok.extern.slf4j.Slf4j;

@ContextConfiguration("classpath:org/alien4cloud/tosca/editor/application-context-test.xml")
@Slf4j
public class EditorStepDefs {

    @Resource
    private ArchiveUploadService csarUploadService;

    @Resource
    private EditorService editorService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private WorkflowsBuilderService workflowBuilderService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Given("^I am authenticated with \"(.*?)\" role$")
    public void i_am_authenticated_with_role(String role) throws Throwable {
        Authentication auth = new TestAuth(role);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String topologyId;

    private AbstractEditorOperation currentOperation;

    private EvaluationContext spelEvaluationContext;

    private Exception thrownException;

    private String lastOperationId;

    private List<Class> typesToClean = new ArrayList<Class>();

    public EditorStepDefs() {
        super();
        typesToClean.add(IndexedArtifactToscaElement.class);
        typesToClean.add(IndexedToscaElement.class);
        typesToClean.add(IndexedCapabilityType.class);
        typesToClean.add(IndexedArtifactType.class);
        typesToClean.add(IndexedRelationshipType.class);
        typesToClean.add(IndexedNodeType.class);
        typesToClean.add(IndexedDataType.class);
        typesToClean.add(PrimitiveIndexedDataType.class);
        typesToClean.add(Csar.class);
    }

    // @Required
    // @Value("${directories.alien}")
    // public void setAlienDirectory(String alienDirectory) {
    // this.alienDirectory = alienDirectory;
    // }

    @Before
    public void init() throws IOException {
        lastOperationId = null;
        thrownException = null;
        for (Class<?> type : typesToClean) {
            alienDAO.delete(type, QueryBuilders.matchAllQuery());
        }
    }

    private static class TestAuth extends UsernamePasswordAuthenticationToken {

        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();

        public TestAuth(String role) {
            super(new User(), null);
            authorities.add(new SimpleGrantedAuthority(role));
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return authorities;
        }

    }

    @Given("^I create an empty topology template$")
    public void i_create_an_empty_topology_template() throws Throwable {
        Topology topology = new Topology();
        topology.setDelegateType(Topology.class.getSimpleName().toLowerCase());
        workflowBuilderService.initWorkflows(workflowBuilderService.buildTopologyContext(topology));
        topologyId = topologyServiceCore.saveTopology(topology);
    }

    @Given("^I upload CSAR from path \"(.*?)\"$")
    public void i_upload_CSAR_from_path(String arg1) throws Throwable {
        csarUploadService.upload(Paths.get(arg1), CSARSource.UPLOAD);
    }

    @Given("^I build the operation: add a node template \"(.*?)\" related to the \"(.*?)\" node type$")
    public void i_build_the_operation_add_a_node_template_related_to_the_node_type(String nodeName, String nodeType) throws Throwable {
        AddNodeOperation operation = new AddNodeOperation();
        operation.setIndexedNodeTypeId(nodeType);
        operation.setNodeName(nodeName);
        currentOperation = operation;
    }

    @Given("^I execute the current operation on the current topology$")
    public void i_execute_the_current_operation_on_the_current_topology() throws Throwable {
        thrownException = null;
        currentOperation.setPreviousOperationId(lastOperationId);
        try {
            TopologyDTO topologyDTO = editorService.execute(topologyId, currentOperation);
            lastOperationId = topologyDTO.getOperations().get(topologyDTO.getLastOperationIndex()).getId();
            spelEvaluationContext = new StandardEvaluationContext(topologyDTO.getTopology());
        } catch (Exception e) {
            log.error("Exception occured while executing operation", e);
            thrownException = e;
        }
    }

    @Given("^I execute the operation$")
    public void i_execute_the_operation(DataTable operationDT) throws Throwable {
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
                parser.parseRaw(operationEntry.getKey()).setValue(operationContext, operationEntry.getValue());
            }
        }

        thrownException = null;
        operation.setPreviousOperationId(lastOperationId);
        try {
            TopologyDTO topologyDTO = editorService.execute(topologyId, operation);
            lastOperationId = topologyDTO.getOperations().get(topologyDTO.getLastOperationIndex()).getId();
            spelEvaluationContext = new StandardEvaluationContext(topologyDTO.getTopology());
        } catch (Exception e) {
            log.error("Exception occured while executing operation", e);
            thrownException = e;
        }
    }

    @Then("^The SPEL boolean expression \"([^\"]*)\" should return true$")
    public void evaluateSpelBooleanExpressionUsingCurrentContext(String spelExpression) {
        Boolean result = (Boolean) evaluateExpression(spelExpression);
        Assert.assertTrue(String.format("The SPEL expression [%s] should return true as a result", spelExpression), result.booleanValue());
    }

    private Object evaluateExpression(String spelExpression) {
        EvaluationContext context = spelEvaluationContext;
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(spelExpression);
        return exp.getValue(context);
    }

    @Then("^The SPEL expression \"([^\"]*)\" should return \"([^\"]*)\"$")
    public void evaluateSpelExpressionUsingCurrentContext(String spelExpression, String expected) {
        Object result = evaluateExpression(spelExpression);
        if ("null".equals(expected)) {
            Assert.assertNull(String.format("The SPEL expression [%s] result should be null", spelExpression), result);
        } else {
            Assert.assertNotNull(String.format("The SPEL expression [%s] result should not be null", spelExpression), result);
            Assert.assertEquals(String.format("The SPEL expression [%s] should return [%s]", spelExpression, expected), expected, result.toString());
        }
    }

    @Then("^The SPEL int expression \"([^\"]*)\" should return (\\d+)$")
    public void The_SPEL_int_expression_should_return(String spelExpression, int expected) throws Throwable {
        Integer actual = (Integer) evaluateExpression(spelExpression);
        Assert.assertEquals(String.format("The SPEL expression [%s] should return [%d]", spelExpression, expected), expected, actual.intValue());
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
            Assert.assertEquals(checkException.getClass(), exceptionClass);
            checkException = checkException.getCause();
        }
    }

    @When("^I build the operation: delete a node template \"(.*?)\" from the topology$")
    public void i_build_the_operation_delete_a_node_template_from_the_topology(String nodeName) throws Throwable {
        DeleteNodeOperation operation = new DeleteNodeOperation();
        operation.setNodeName(nodeName);
        currentOperation = operation;
    }

    @When("^I build the operation: add a relationship of type \"(.*?)\" defined in archive \"(.*?)\" version \"(.*?)\" with source \"(.*?)\" and target \"(.*?)\" for requirement \"(.*?)\" of type \"(.*?)\" and target capability \"(.*?)\"$")
    public void i_build_the_operation_add_a_relationship_of_type_defined_in_archive_version_with_source_and_target_for_requirement_of_type_and_target_capability(
            String type, String csar, String version, String source, String target, String req, String reqType, String capability) throws Throwable {
        AddRelationshipOperation operation = new AddRelationshipOperation();
        operation.setNodeName(source);
        operation.setRelationshipType(type);
        operation.setRelationshipVersion(version);
        operation.setRequirementName(req);
        operation.setRequirementType(reqType);
        operation.setTarget(target);
        operation.setTargetedCapabilityName(capability);
        currentOperation = operation;
    }

}
