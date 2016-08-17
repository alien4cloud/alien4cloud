package alien4cloud.it.topology;

import java.util.Map;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.google.common.collect.Maps;

import alien4cloud.it.Context;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import gherkin.formatter.model.DataTableRow;

/**
 * Steps to manage the editor.
 */
public class EditorStepDefinitions {
    // Keep a local context for topology dto
    private static TopologyDTO TOPOLOGY_DTO = null;

    @Given("^I get the topology")
    public void i_get_the_topology() throws Throwable {
        // Call the rest controller to get the topology DTO and register it
        String topologyId = Context.getInstance().getTopologyId();

        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/topologies/" + topologyId));
        trySetTopologyDto();
    }

    @Given("^I execute the operation$")
    public void i_execute_the_operation(DataTable operationDT) throws Throwable {
        String topologyId = Context.getInstance().getTopologyId();

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

        if (TOPOLOGY_DTO == null || TOPOLOGY_DTO.getLastOperationIndex() == -1) {
            // no previous operations
            operation.setPreviousOperationId(null);
        } else {
            operation.setPreviousOperationId(TOPOLOGY_DTO.getOperations().get(TOPOLOGY_DTO.getLastOperationIndex()).getId());
        }

        // Call execute rest service and set the topology DTO to the context
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v2/editor/" + topologyId + "/execute"));
        trySetTopologyDto();
    }

    private void trySetTopologyDto() {
        try {
            TOPOLOGY_DTO = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();
        } catch (Exception e) {
            // This may fail as the latest rest call may fail based on test scenario but this is a shortcut for all successfull scenario.
        }
    }
}
