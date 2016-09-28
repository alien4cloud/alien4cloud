package alien4cloud.utils.services;

import java.util.HashMap;
import java.util.Map;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.junit.Assert;
import org.junit.Test;

import alien4cloud.common.AlienConstants;
import alien4cloud.topology.TopologyUtils;

public class TopologyServiceTest {

    @Test
    public void isValidNodeName() {
        Assert.assertTrue(TopologyUtils.isValidNodeName("Compute"));
        Assert.assertTrue(TopologyUtils.isValidNodeName("Compute_2"));

        Assert.assertFalse(TopologyUtils.isValidNodeName("Computé"));
        Assert.assertFalse(TopologyUtils.isValidNodeName("Compute-2"));
        Assert.assertFalse(TopologyUtils.isValidNodeName("Compute.unix"));
        Assert.assertFalse(TopologyUtils.isValidNodeName("Compute 2"));
    }

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

        TopologyUtils.normalizeAllNodeTemplateName(topology, null);
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_2"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_21"));
        Assert.assertTrue(topology.getNodeTemplates().containsKey("Compute_22"));
    }
}
