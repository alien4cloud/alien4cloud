package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.assertj.core.api.Assertions;
import org.elasticsearch.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class DefaultNodeMatcherTest {

    private INodeMatcherPlugin nodeMatcher;

    @Before
    public void setUp() throws Exception {
        this.nodeMatcher = new DefaultNodeMatcher();
    }

    @Test
    public void a_valid_location_resource_compute_should_be_able_to_match_an_abstract_compute_from_topology() throws Exception {
        // Given
        NodeType computeNodeType = new NodeType();
        computeNodeType.setAbstract(true);
        computeNodeType.setElementId("tosca.nodes.Compute");

        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setType("tosca.nodes.Compute");

        LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
        availableNodeTemplate1.setTemplate(nodeTemplate1);

        LocationResources locationResources = new LocationResources();
        locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
        locationResources.setNodeTypes(ImmutableMap.of("tosca.nodes.Compute", computeNodeType));

        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wishedNodeTemplate = new NodeTemplate();
        wishedNodeTemplate.setType("tosca.nodes.Compute");
        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wishedNodeTemplate, computeNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        Assertions.assertThat(proposition).hasSize(1);
    }

    @Test
    public void unknown_type_from_topology_should_not_match_location_resource() throws Exception {
        // Given
        NodeType computeNodeType = new NodeType();
        computeNodeType.setAbstract(true);
        computeNodeType.setElementId("tosca.nodes.Compute");

        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setType("tosca.nodes.Compute");

        LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
        availableNodeTemplate1.setTemplate(nodeTemplate1);

        LocationResources locationResources = new LocationResources();
        locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
        locationResources.setNodeTypes(ImmutableMap.of("tosca.nodes.Compute", computeNodeType));

        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wishedNodeTemplate = new NodeTemplate();
        wishedNodeTemplate.setType("tosca.nodes.Unknown");
        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wishedNodeTemplate, computeNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        Assertions.assertThat(proposition).hasSize(0);
    }

    @Test
    public void location_resource_should_be_matched_only_if_capabilities_are_satisfied() throws Exception {
        // Given
        CapabilityDefinition capabilityArchitecture = new CapabilityDefinition("tosca.capabilities.OperatingSystem");
        capabilityArchitecture.setProperties(ImmutableMap.of( //
                "architecture", Arrays.asList("x86", "arm"), //
                "distribution", Arrays.asList("windows", "linux")));

        NodeType computeNodeType = new NodeType();
        computeNodeType.setAbstract(true);
        computeNodeType.setElementId("tosca.nodes.Compute");
        computeNodeType.setCapabilities(Arrays.asList(capabilityArchitecture));

        NodeTemplate nodeTemplate1 = new NodeTemplate();
        nodeTemplate1.setType("tosca.nodes.Compute");

        LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
        availableNodeTemplate1.setTemplate(nodeTemplate1);

        LocationResources locationResources = new LocationResources();
        locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
        locationResources.setNodeTypes(ImmutableMap.of("tosca.nodes.Compute", computeNodeType));

        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wishedNodeTemplate = new NodeTemplate();
        wishedNodeTemplate.setType("tosca.nodes.Compute");
        Capability wishedCapability = new Capability();
        wishedCapability.setType("proot");
        wishedCapability.setProperties(ImmutableMap.of("yolo", new AbstractPropertyValue() {
            @Override
            public boolean isDefinition() {
                return true;
            }
        }));
        wishedNodeTemplate.setCapabilities(ImmutableMap.of("proot", wishedCapability));
        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wishedNodeTemplate, computeNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        Assertions.assertThat(proposition).hasSize(1);
    }
}