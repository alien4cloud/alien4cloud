package alien4cloud.tosca.container;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.tosca.container.ToscaFunctionProcessor;
import alien4cloud.tosca.container.model.topology.NodeTemplate;
import alien4cloud.tosca.container.model.topology.Topology;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Maps;

public class FunctionProcessorTest {

    @Test
    public void test() {
        Map<String, NodeTemplate> nodeTemplates = Maps.newHashMap();
        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_1" }, new String[] { "the_property_value_1" }));
        nodeTemplates.put("the_node_tempalte_1", nodeTemplate1);
        NodeTemplate nodeTemplate2 = new NodeTemplate();
        nodeTemplate2.setProperties(MapUtil.newHashMap(new String[] { "the_property_name_2" }, new String[] { "the_property_value_2" }));
        nodeTemplates.put("the_node_tempalte_2", nodeTemplate2);
        Topology topology = new Topology();
        topology.setNodeTemplates(nodeTemplates);

        Map<String, Map<Integer, InstanceInformation>> runtimeInformations = Maps.newHashMap();

        String parsedString = ToscaFunctionProcessor.parseString(
                "http://get_property: [the_node_tempalte_1, the_property_name_1]:get_property: [the_node_tempalte_2, the_property_name_2 ]/super", topology,
                runtimeInformations, 0);
        Assert.assertEquals("http://the_property_value_1:the_property_value_2/super", parsedString);
    }
}