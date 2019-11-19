package alien4cloud.deployment.matching.services.nodes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.deployment.matching.MatchingFilterDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;

public class DefaultNodeMatcherTest {

    private INodeMatcherPlugin nodeMatcher;

    private NodeType computeNodeType;

    private NodeTemplate computeNodeTemplate;

    private LocationResources locationResources;

    @Before
    public void setUp() throws Exception {
        this.nodeMatcher = new DefaultNodeMatcher();

        this.computeNodeType = nodeType("org.alien4cloud.nodes.mock.aws.Compute", "tosca.nodes.Compute");
        this.computeNodeTemplate = nodeTemplate("org.alien4cloud.nodes.mock.aws.Compute");

        NodeType mongoDbNodeType = nodeType("alien.service.MongoDB", "", "tosca.service.ServiceType", "test.nodes.DB");
        NodeTemplate mongoDbNodeTemplate = nodeTemplate("alien.service.MongoDB");

        LocationResourceTemplate mongoDbLocationTemplate = locationResourceTemplate(mongoDbNodeTemplate);
        mongoDbLocationTemplate.setService(true);

        locationResources = new LocationResources();
        locationResources.setNodeTemplates(//
                Arrays.asList(//
                        locationResourceTemplate(computeNodeTemplate), //
                        mongoDbLocationTemplate//
                ));
        locationResources.setNodeTypes( //
                ImmutableMap.of( //
                        computeNodeType.getElementId(), computeNodeType, //
                        mongoDbNodeType.getElementId(), mongoDbNodeType //
                ));
    }

    @Test
    public void real_world_location_resource_compute_should_be_able_to_match_an_abstract_compute_from_topology() throws Exception {
        // Given
        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wantedNodeTemplate = nodeTemplate("tosca.nodes.Compute");

        NodeType wantedNodeType = new NodeType();

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, wantedNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        assertThat(proposition).hasSize(1);
    }

    @Test
    public void unknown_type_from_topology_should_not_match_any_location_resource() throws Exception {
        // Given
        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wantedNodeTemplate = nodeTemplate("tosca.nodes.Unknown");

        NodeType wantedNodeType = new NodeType();

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, wantedNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        assertThat(proposition).hasSize(0);
    }

    @Test
    public void location_resource_should_be_matched_only_if_capabilities_are_satisfied() throws Exception {
        // Given
        CapabilityDefinition capabilityArchitecture = new CapabilityDefinition("tosca.capabilities.OperatingSystem");
        computeNodeType.setCapabilities(Arrays.asList(capabilityArchitecture));

        Capability capability = new Capability();
        capability.setType("tosca.capabilities.OperatingSystem");
        capability.setProperties(ImmutableMap.of("architecture", new ScalarPropertyValue("x86")));
        computeNodeTemplate.setCapabilities(ImmutableMap.of("os", capability));

        CapabilityType capabilityType = new CapabilityType();
        PropertyDefinition propertyDefinition = new PropertyDefinition();
        propertyDefinition.setType("string");
        capabilityType.setElementId("tosca.capabilities.OperatingSystem");
        capabilityType.setProperties(ImmutableMap.of("architecture", propertyDefinition));
        locationResources.setCapabilityTypes(ImmutableMap.of("tosca.capabilities.OperatingSystem", capabilityType));

        // Matching configuration
        Map<String, MatchingConfiguration> capabilityFilterConfiguration = new HashMap<>();
        MatchingConfiguration matchingConfiguration = new MatchingConfiguration();
        MatchingFilterDefinition matchingFilterDefinition = new MatchingConfiguration();
        matchingFilterDefinition.setProperties(ImmutableMap.of("architecture", Arrays.asList(new EqualConstraint())));
        matchingConfiguration.setCapabilities(ImmutableMap.of("os", matchingFilterDefinition));
        capabilityFilterConfiguration.put("org.alien4cloud.nodes.mock.aws.Compute", matchingConfiguration);

        // When
        NodeTemplate wantedNodeTemplate = new NodeTemplate();
        wantedNodeTemplate.setType("tosca.nodes.Compute");
        Capability wantedCapability = new Capability();
        wantedCapability.setType("tosca.capabilities.OperatingSystem");
        wantedCapability.setProperties(ImmutableMap.of("architecture", new ScalarPropertyValue("power_pc")));
        wantedNodeTemplate.setCapabilities(ImmutableMap.of("os", wantedCapability));

        NodeType nodeType = new NodeType();

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, nodeType, locationResources, capabilityFilterConfiguration);

        // Then
        assertThat(proposition).hasSize(0);
    }

    @Test
    public void abstract_template_should_be_matched_if_service_is_available() throws Exception {
        // Given
        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wantedNodeTemplate = nodeTemplate("test.nodes.DB");

        NodeType wantedNodeType = new NodeType();
        wantedNodeType.setAbstract(true);

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, wantedNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        assertThat(proposition).hasSize(1);
        assertThat(proposition.get(0).isService()).isTrue();
    }

    @Test
    @Ignore // we do matching even on concrete nodes
    public void concrete_template_cannot_be_matched_even_if_service_is_available() throws Exception {
        // Given
        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wantedNodeTemplate = nodeTemplate("test.nodes.DB");

        NodeType wantedNodeType = new NodeType();
        wantedNodeType.setAbstract(false);

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, wantedNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        assertThat(proposition).hasSize(0);
    }

    @Test
    public void abstract_template_should_be_matched_if_service_is_available_2() throws Exception {
        // Given
        Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

        // When
        NodeTemplate wantedNodeTemplate = nodeTemplate("alien.service.MongoDB");

        NodeType wantedNodeType = new NodeType();
        wantedNodeType.setAbstract(true);

        List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, wantedNodeType, locationResources, emptyMatchingConfigurations);

        // Then
        assertThat(proposition).hasSize(1);
        assertThat(proposition.get(0).isService()).isTrue();
    }

    private NodeType nodeType(String elementId, String... derivedTypes) {
        NodeType nodeType = new NodeType();
        nodeType.setAbstract(false);
        nodeType.setElementId(elementId);
        nodeType.setDerivedFrom(Arrays.asList(derivedTypes));
        return nodeType;
    }

    private NodeTemplate nodeTemplate(String type) {
        NodeTemplate nodeTemplate = new NodeTemplate();
        nodeTemplate.setType(type);
        return nodeTemplate;
    }

    private LocationResourceTemplate locationResourceTemplate(NodeTemplate nodeTemplate) {
        LocationResourceTemplate locationResourceTemplate = new LocationResourceTemplate();
        locationResourceTemplate.setTemplate(nodeTemplate);
        return locationResourceTemplate;
    }

}