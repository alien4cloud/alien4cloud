package alien4cloud.it.topology;


import org.alien4cloud.tosca.editor.operations.secrets.SetNodeCapabilityPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.operations.secrets.SetNodePropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.operations.secrets.UnsetNodeCapabilityPropertyAsSecretOperation;
import org.alien4cloud.tosca.editor.operations.secrets.UnsetNodePropertyAsSecretOperation;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.junit.Assert;

import com.fasterxml.jackson.databind.JavaType;

import alien4cloud.it.Context;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.And;
import cucumber.api.java.en.When;

public class SecretPropertiesStepDefinitions {

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as secret with a secret path \"([^\"]*)\"$")
    public void iDefineThePropertyOfTheNodeAsSecretWithASecretPath(String propertyName, String nodeName, String secretPath) throws Throwable {
        SetNodePropertyAsSecretOperation operation = new SetNodePropertyAsSecretOperation();
        operation.setPropertyName(propertyName);
        operation.setNodeName(nodeName);
        operation.setSecretPath(secretPath);
        EditorStepDefinitions.do_i_execute_the_operation(operation);
    }

    @When("^I define the property \"([^\"]*)\" of the node \"([^\"]*)\" as secret with a secret path \"([^\"]*)\" and I save the topology$")
    public void iDefineThePropertyOfTheNodeAsSecretWithASecretPathAndISave(String propertyName, String nodeName, String secretPath) throws Throwable {
        iDefineThePropertyOfTheNodeAsSecretWithASecretPath(propertyName, nodeName, secretPath);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @And("^The topology should have the property \"([^\"]*)\" of a node \"([^\"]*)\" defined as a secret with a secret path \"([^\"]*)\"$")
    public void theTopologyShouldHaveThePropertyDefinedAsASecretWithASecretPath(String propertyName, String nodeName, String secretPath) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        JavaType restResponseType = Context.getJsonMapper().getTypeFactory().constructParametricType(RestResponse.class, TopologyDTO.class);
        TopologyDTO topologyDTO = ((RestResponse<TopologyDTO>) Context.getJsonMapper().readValue(response, restResponseType)).getData();
        FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) topologyDTO.getTopology().getNodeTemplates().get(nodeName).getProperties().get(propertyName);
        Assert.assertEquals(secretPath, functionPropertyValue.getParameters().get(0));
    }

    @When("^I define the property \"([^\"]*)\" of capability \"([^\"]*)\" of the node \"([^\"]*)\" as secret with a secret path \"([^\"]*)\"$")
    public void iDefineThePropertyOfCapabilityOfTheNodeAsSecretWithASecretPath(String propertyName, String capabilityName, String nodeName, String secretPath) throws Throwable {
        SetNodeCapabilityPropertyAsSecretOperation operation = new SetNodeCapabilityPropertyAsSecretOperation();
        operation.setNodeName(nodeName);
        operation.setPropertyName(propertyName);
        operation.setSecretPath(secretPath);
        operation.setCapabilityName(capabilityName);
        EditorStepDefinitions.do_i_execute_the_operation(operation);
    }

    @When("^I define the property \"([^\"]*)\" of capability \"([^\"]*)\" of the node \"([^\"]*)\" as secret with a secret path \"([^\"]*)\" and I save the topology$")
    public void iDefineThePropertyOfCapabilityOfTheNodeAsSecretWithASecretPathAndISaveTheTopology(String propertyName, String capabilityName, String nodeName, String secretPath) throws Throwable {
        iDefineThePropertyOfCapabilityOfTheNodeAsSecretWithASecretPath(propertyName, capabilityName, nodeName, secretPath);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @And("^The topology should have the property \"([^\"]*)\" of capability \"([^\"]*)\" of a node \"([^\"]*)\" defined as a secret with a secret path \"([^\"]*)\"$")
    public void theTopologyShouldHaveThePropertyOfCapabilityOfANodeDefinedAsASecretWithASecretPath(String propertyName, String capabilityName, String nodeName, String secretPath) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/topologies/" + Context.getInstance().getTopologyId());
        JavaType restResponseType = Context.getJsonMapper().getTypeFactory().constructParametricType(RestResponse.class, TopologyDTO.class);
        TopologyDTO topologyDTO = ((RestResponse<TopologyDTO>) Context.getJsonMapper().readValue(response, restResponseType)).getData();
        FunctionPropertyValue functionPropertyValue = (FunctionPropertyValue) topologyDTO.getTopology().getNodeTemplates().get(nodeName).getCapabilities().get(capabilityName).getProperties().get(propertyName);
        Assert.assertEquals(secretPath, functionPropertyValue.getParameters().get(0));
    }

    @When("^I unset the property \"([^\"]*)\" of the node \"([^\"]*)\" back to normal value$")
    public void iUnsetThePropertyOfTheNodeBackToNormalValue(String propertyName, String nodeName) throws Throwable {
        UnsetNodePropertyAsSecretOperation operation = new UnsetNodePropertyAsSecretOperation();
        operation.setPropertyName(propertyName);
        operation.setNodeName(nodeName);
        EditorStepDefinitions.do_i_execute_the_operation(operation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

    @When("^I unset the property \"([^\"]*)\" of capability \"([^\"]*)\" of the node \"([^\"]*)\" back to normal value$")
    public void iUnsetThePropertyOfCapabilityOfTheNodeBackToNormalValue(String propertyName, String capabilityName, String nodeName) throws Throwable {
        UnsetNodeCapabilityPropertyAsSecretOperation operation = new UnsetNodeCapabilityPropertyAsSecretOperation();
        operation.setNodeName(nodeName);
        operation.setPropertyName(propertyName);
        operation.setCapabilityName(capabilityName);
        EditorStepDefinitions.do_i_execute_the_operation(operation);
        EditorStepDefinitions.do_i_save_the_topology();
    }

}
