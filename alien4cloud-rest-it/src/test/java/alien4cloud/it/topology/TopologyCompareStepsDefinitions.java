package alien4cloud.it.topology;

import static alien4cloud.it.Context.getRestClientInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.alien4cloud.tosca.model.templates.NodeTemplate;

import alien4cloud.it.Context;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.topology.TopologyDTO;
import cucumber.api.java.en.Then;

/**
 *
 */
public class TopologyCompareStepsDefinitions {
    @Then("^Topologies \"([^\"]*)\" and \"([^\"]*)\" have the same number of node templates with identical types.$")
    public void quickCompareTopologies(String topologyId1, String topologyId2) throws Throwable {
        // created topology
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/topologies/" + topologyId1));
        TopologyDTO topology1 = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();

        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/topologies/" + topologyId2));
        TopologyDTO topology2 = JsonUtil.read(Context.getInstance().getRestResponse(), TopologyDTO.class, Context.getJsonMapper()).getData();

        // node templates count test
        assertNotNull(topology2);
        assertNotNull(topology1);
        int topo2nodeCount = topology2.getTopology() == null || topology2.getTopology().getNodeTemplates() == null ? 0
                : topology2.getTopology().getNodeTemplates().size();
        int topo1nodeCount = topology1.getTopology() == null || topology1.getTopology().getNodeTemplates() == null ? 0
                : topology1.getTopology().getNodeTemplates().size();
        assertEquals(topo2nodeCount, topo1nodeCount);

        if (topo2nodeCount == 0) {
            return;
        }
        // node templates name / type test
        for (Map.Entry<String, NodeTemplate> entry : topology1.getTopology().getNodeTemplates().entrySet()) {
            assertTrue(topology2.getTopology().getNodeTemplates().containsKey(entry.getKey()));
            assertTrue(topology2.getTopology().getNodeTemplates().get(entry.getKey()).getType().equals(entry.getValue().getType()));
        }
    }
}