package alien4cloud.tosca.parser.postprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class CapabilityMatcherServiceTest {

    private CapabilityMatcherService service;
    private NodeTemplate nodeTemplate;
    private Map<String, CapabilityType> capabilityTypeByTypeName;

    @Mock
    private CapabilityMatcherService.IToscaContextFinder toscaContextFinder;

    @Before
    public void setUp() throws Exception {
        service = new CapabilityMatcherService();
        service.setToscaContextFinder(toscaContextFinder);

        nodeTemplate = new NodeTemplate();
        nodeTemplate.setCapabilities(Maps.newHashMap());

        capabilityTypeByTypeName = Maps.newHashMap();
        addCapabilityToNodeTemplateAndToscaContext("db_endpoint","alien.capability.test.MongoEndpoint", "alien.capability.test.Database", "alien.capability.test.Endpoint");
        addCapabilityToNodeTemplateAndToscaContext("useless","alien.capability.test.Alone");

        Mockito.when(toscaContextFinder.find(Mockito.any(), Mockito.anyString()))
                .then(invocationOnMock -> capabilityTypeByTypeName.get(invocationOnMock.getArguments()[1]));
    }

    @Test
    public void capability_should_match_when_type_equals_element_id() throws Exception {
        Map<String, Capability> compatibleCapabilities = service.getCompatibleCapabilityByType(nodeTemplate, "alien.capability.test.MongoEndpoint");

        assertThat(compatibleCapabilities).hasSize(1);
        assertThat(compatibleCapabilities).containsKeys("db_endpoint");
    }

    @Test
    public void capability_should_match_when_type_is_present_in_derived_from() throws Exception {
        Map<String, Capability> compatibleCapabilities = service.getCompatibleCapabilityByType(nodeTemplate, "alien.capability.test.Database");

        assertThat(compatibleCapabilities).hasSize(1);
        assertThat(compatibleCapabilities).containsKeys("db_endpoint");
    }

    @Test
    public void capability_should_not_match_when_type_is_unknown() throws Exception {
        Map<String, Capability> compatibleCapabilities = service.getCompatibleCapabilityByType(nodeTemplate, "alien.capability.test.Unknown");

        assertThat(compatibleCapabilities).hasSize(0);
    }

    @Test
    public void should_not_crash_even_if_node_template_capacities_is_incorrect() throws Exception {
        nodeTemplate.getCapabilities().clear();
        Capability unknownCapacity = new Capability();
        unknownCapacity.setType("alien.typo.in.capa");
        nodeTemplate.getCapabilities().put("incorrect", unknownCapacity);

        Map<String, Capability> compatibleCapabilities = service.getCompatibleCapabilityByType(nodeTemplate, "alien.capability.test.Unknown");

        assertThat(compatibleCapabilities).hasSize(0);
    }


    private void addCapabilityToNodeTemplateAndToscaContext(String name, String type, String... derivedFrom) {
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setDerivedFrom(Arrays.asList(derivedFrom));
        capabilityType.setElementId(type);
        capabilityTypeByTypeName.put(type, capabilityType);

        Capability capability = new Capability();
        capability.setType(type);

        nodeTemplate.getCapabilities().put(name, capability);
    }

}