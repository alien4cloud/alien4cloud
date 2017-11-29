package alien4cloud.utils.services;

import java.util.HashMap;
import java.util.Map;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.junit.Assert;
import org.junit.Test;

import org.alien4cloud.tosca.utils.TopologyUtils;
import alien4cloud.utils.AlienConstants;

public class TopologyServiceTest {

    @Test
    public void normalizeAllNodeTemplateName() {
        Topology topology = new Topology();
        topology.setArchiveName("test-topology");
        topology.setArchiveVersion("1.0.0");
        topology.setWorkspace(AlienConstants.GLOBAL_WORKSPACE_ID);
        Map<String, NodeTemplate> nodeTemplates = new HashMap<>();
        nodeTemplates.put("Computé", new NodeTemplate());
        nodeTemplates.put("Compute-2", new NodeTemplate());
        nodeTemplates.put("Compute.2", new NodeTemplate());
        nodeTemplates.put("Compute 2", new NodeTemplate());
        topology.setNodeTemplates(nodeTemplates);

        TopologyUtils.normalizeAllNodeTemplateName(topology, null, null);
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_2"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_21"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_22"));
    }
}
