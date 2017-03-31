package alien4cloud.tosca;

import java.util.Map;

import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.normative.constants.ToscaFunctionConstants;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.utils.AlienUtils;
import alien4cloud.utils.MapUtil;

/**
 * Created by igor on 27/03/17.
 */
public class PaaSUtilsTest {

    private final String SELF = "SELF_";

    private final String interface1 = "interface1";
    private final String operation1 = "operation1";
    private final String fake1 = "FAKE1";
    private final String fake2 = "FAKE2";
    private final String fake3 = "FAKE3";
    private final String fake4 = "FAKE4";
    private final String fake5 = "FAKE5";

    private final String fakeCapa1 = "fakecapa1";

    private final String node1 = "node1";
    private final String node2 = "node2";

    @Test
    public void injectNodeTemplatePropertiesAsInputs() throws Exception {
        // NodeType nodeType = buildFakeNodeType("test.node.fake1", "1");

        PaaSNodeTemplate paaSNodeTemplate = buildPaaSNodeTemplate();

        PaaSUtils.processNodeTemplateProperties(paaSNodeTemplate);

        Operation operation = paaSNodeTemplate.getInterfaces().get(interface1).getOperations().get(operation1);
        Assert.assertNotNull(operation.getInputParameters());

        // assert all node properties are inputs properties
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake1));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake5));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake2));
        // complex property should not be there
        Assert.assertFalse(operation.getInputParameters().containsKey(fake3));

        // assert that the property from the operation has not been overrided
        Assert.assertEquals(operation.getInputParameters().get(fake1), new ScalarPropertyValue("1_from_operation"));

        // check capabilities inputs
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + generateCapaInputName(fakeCapa1, fake1)));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + generateCapaInputName(fakeCapa1, fake3)));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + generateCapaInputName(fakeCapa1, fake2)));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + generateCapaInputName(fakeCapa1, fake5)));
    }

    @Test
    public void injectRelationshipTemplatePropertiesAsInputs() throws Exception {

        Map<String, PaaSNodeTemplate> nodes = Maps.newHashMap();
        PaaSNodeTemplate source = buildPaaSNodeTemplate();
        nodes.put(node1, source);
        PaaSNodeTemplate target = buildPaaSNodeTemplate();
        nodes.put(node2, target);

        PaaSRelationshipTemplate paaSRelationshipTemplate = buildFakePaaSRelTemplate(node1, node2, fakeCapa1);
        Mockito.when(source.getRelationshipTemplates()).thenReturn(Lists.newArrayList(paaSRelationshipTemplate));

        PaaSUtils.processRelationshipTemplateProperties(paaSRelationshipTemplate, nodes);

        Operation operation = paaSRelationshipTemplate.getInterfaces().get(interface1).getOperations().get(operation1);
        Assert.assertNotNull(operation.getInputParameters());

        // assert all relationship properties are inputs properties
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake1));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake3));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake5));
        Assert.assertTrue(operation.getInputParameters().containsKey(SELF + fake2));

        // assert that the property from the operation has not been overrided
        Assert.assertEquals(operation.getInputParameters().get(fake1), new ScalarPropertyValue("1_from_operation"));

        // check source node inputs
        Assert.assertTrue(operation.getInputParameters().containsKey(generateSourceInputName(fake1)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateSourceInputName(fake2)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateSourceInputName(fake3)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateSourceInputName(fake5)));

        // check target node inputs
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetInputName(fake1)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetInputName(fake2)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetInputName(fake3)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetInputName(fake5)));

        // check targeted capability inputs
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetedCapaInputName(fakeCapa1, fake1)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetedCapaInputName(fakeCapa1, fake2)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetedCapaInputName(fakeCapa1, fake5)));
        Assert.assertTrue(operation.getInputParameters().containsKey(generateTargetedCapaInputName(fakeCapa1, fake3)));
    }

    private PaaSNodeTemplate buildPaaSNodeTemplate() {
        Map<String, Capability> capabilities = Maps.newHashMap();
        capabilities.put(fakeCapa1, buildFakeCapability());
        NodeTemplate nodeTemplate = buildFakeNodeTemplate(capabilities);

        Map<String, Operation> operations = Maps.newHashMap();
        operations.put(operation1, buildFakeOperation());

        Map<String, Interface> interfaces = Maps.newHashMap();
        interfaces.put(interface1, buildFakeInterface(operations));

        return buildFakePaaSNodeTemplate(nodeTemplate, interfaces);
    }

    private PaaSNodeTemplate buildFakePaaSNodeTemplate(NodeTemplate nodeTemplate, Map<String, Interface> interfaces) {
        PaaSNodeTemplate fakePaaSNodeTemplate = Mockito.mock(PaaSNodeTemplate.class);
        Mockito.when(fakePaaSNodeTemplate.getTemplate()).thenReturn(nodeTemplate);
        Mockito.when(fakePaaSNodeTemplate.getInterfaces()).thenReturn(interfaces);
        return fakePaaSNodeTemplate;
    }

    private NodeTemplate buildFakeNodeTemplate(Map<String, Capability> capabilities) {
        Map<String, AbstractPropertyValue> props = Maps.newHashMap();
        props.put(fake1, new ScalarPropertyValue("1_from_nodeTemplate"));
        props.put(fake2, null);
        props.put(fake3, new ComplexPropertyValue(MapUtil.newHashMap(new String[] { "toto" }, new String[] { "tata" })));
        props.put(fake5, new ScalarPropertyValue("5_from_nodeTemplate"));
        NodeTemplate fakeTemplate = Mockito.mock(NodeTemplate.class);
        Mockito.when(fakeTemplate.getProperties()).thenReturn(props);
        Mockito.when(fakeTemplate.getCapabilities()).thenReturn(capabilities);
        return fakeTemplate;
    }

    private Capability buildFakeCapability() {
        Map<String, AbstractPropertyValue> props = Maps.newHashMap();
        props.put(fake1, new ScalarPropertyValue("1_capa"));
        props.put(fake2, null);
        props.put(fake3, new ComplexPropertyValue(MapUtil.newHashMap(new String[] { "toto" }, new String[] { "tata" })));
        props.put(fake5, new ScalarPropertyValue("5_capa"));
        Capability fakeCapability = Mockito.mock(Capability.class);
        Mockito.when(fakeCapability.getProperties()).thenReturn(props);
        return fakeCapability;
    }

    private RelationshipTemplate buildFakeRelationShip(String target, String targetedCapability) {
        Map<String, AbstractPropertyValue> props = Maps.newHashMap();
        props.put(fake1, new ScalarPropertyValue("1_rel"));
        props.put(fake2, null);
        props.put(fake3, new ComplexPropertyValue(MapUtil.newHashMap(new String[] { "toto" }, new String[] { "tata" })));
        props.put(fake5, new ScalarPropertyValue("5_rel"));
        RelationshipTemplate fakeRel = Mockito.mock(RelationshipTemplate.class);
        Mockito.when(fakeRel.getTarget()).thenReturn(target);
        Mockito.when(fakeRel.getTargetedCapabilityName()).thenReturn(targetedCapability);
        Mockito.when(fakeRel.getProperties()).thenReturn(props);
        return fakeRel;
    }

    private PaaSRelationshipTemplate buildFakePaaSRelTemplate(String source, String target, String targetedCapability) {

        PaaSRelationshipTemplate fakePaaSRelTemplate = Mockito.mock(PaaSRelationshipTemplate.class);
        RelationshipTemplate relationshipTemplate = buildFakeRelationShip(target, targetedCapability);
        Mockito.when(fakePaaSRelTemplate.getTemplate()).thenReturn(relationshipTemplate);

        Map<String, Operation> operations = Maps.newHashMap();
        operations.put(operation1, buildFakeOperation());

        Map<String, Interface> interfaces = Maps.newHashMap();
        interfaces.put(interface1, buildFakeInterface(operations));
        Mockito.when(fakePaaSRelTemplate.getInterfaces()).thenReturn(interfaces);
        Mockito.when(fakePaaSRelTemplate.getSource()).thenReturn(source);
        return fakePaaSRelTemplate;
    }

    private Operation buildFakeOperation() {
        Operation fakeOperation = Mockito.mock(Operation.class);
        Map<String, IValue> props = Maps.newHashMap();
        props.put(fake1, new ScalarPropertyValue("1_from_operation"));
        props.put(fake4, new ScalarPropertyValue("fake4"));
        Mockito.when(fakeOperation.getInputParameters()).thenReturn(props);
        return fakeOperation;
    }

    private Interface buildFakeInterface(Map<String, Operation> operations) {
        Interface fakeInterface = Mockito.mock(Interface.class);
        Mockito.when(fakeInterface.getOperations()).thenReturn(operations);
        return fakeInterface;
    }

    private String generateCapaInputName(String capaName, String propertyName) {
        return StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, PaaSUtils.CAPABILITIES, capaName, propertyName);
    }

    private String generateSourceInputName(String propertyName) {
        return StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.SOURCE, propertyName);
    }

    private String generateTargetInputName(String propertyName) {
        return StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.TARGET, propertyName);
    }

    private String generateTargetedCapaInputName(String capaName, String propertyName) {
        return StringUtils.joinWith(AlienUtils.DEFAULT_PREFIX_SEPARATOR, ToscaFunctionConstants.TARGET, PaaSUtils.CAPABILITIES, capaName, propertyName);
    }

}