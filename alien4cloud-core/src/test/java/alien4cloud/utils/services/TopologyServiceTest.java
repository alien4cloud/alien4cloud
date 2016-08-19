package alien4cloud.utils.services;

import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.Topology;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.topology.TopologyUtils;

import java.util.HashMap;
import java.util.Map;

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
