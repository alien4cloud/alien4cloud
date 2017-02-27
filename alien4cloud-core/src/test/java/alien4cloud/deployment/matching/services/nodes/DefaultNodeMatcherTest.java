package alien4cloud.deployment.matching.services.nodes;

import alien4cloud.deployment.matching.plugins.INodeMatcherPlugin;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.deployment.matching.MatchingFilterDefinition;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.model.orchestrators.locations.LocationResources;
import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.definitions.constraints.EqualConstraint;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.assertj.core.api.Assertions;
import org.elasticsearch.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.*;

public class DefaultNodeMatcherTest {

	private INodeMatcherPlugin nodeMatcher;

	@Before
	public void setUp() throws Exception {
		this.nodeMatcher = new DefaultNodeMatcher();
	}

	@Test
	public void real_world_location_resource_compute_should_be_able_to_match_an_abstract_compute_from_topology() throws Exception {
		// Given
		NodeType computeNodeType = new NodeType();
		computeNodeType.setAbstract(true);
		computeNodeType.setElementId("alien.nodes.mock.aws.Compute");
		computeNodeType.setDerivedFrom(Arrays.asList("tosca.nodes.Compute"));

		NodeTemplate nodeTemplate1 = new NodeTemplate();
		nodeTemplate1.setType("alien.nodes.mock.aws.Compute");

		LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
		availableNodeTemplate1.setTemplate(nodeTemplate1);

		LocationResources locationResources = new LocationResources();
		locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
		locationResources.setNodeTypes(ImmutableMap.of("alien.nodes.mock.aws.Compute", computeNodeType));

		Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

		// When
		NodeTemplate wantedNodeTemplate = new NodeTemplate();
		wantedNodeTemplate.setType("tosca.nodes.Compute");
		List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, null,
				locationResources, emptyMatchingConfigurations);

		// Then
		assertThat(proposition).hasSize(1);
	}


	@Test
	public void naive_location_resource_compute_should_be_able_to_match_an_abstract_compute_from_topology() throws Exception {
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
		NodeTemplate wantedNodeTemplate = new NodeTemplate();
		wantedNodeTemplate.setType("tosca.nodes.Compute");
		List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, null,
				locationResources, emptyMatchingConfigurations);

		// Then
		assertThat(proposition).hasSize(1);
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
		NodeTemplate wantedNodeTemplate = new NodeTemplate();
		wantedNodeTemplate.setType("tosca.nodes.Unknown");
		List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, null,
				locationResources, emptyMatchingConfigurations);

		// Then
		assertThat(proposition).hasSize(0);
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
		Capability capability = new Capability();
		capability.setType("tosca.capabilities.OperatingSystem");
		capability.setProperties(ImmutableMap.of("architecture", new ScalarPropertyValue("x86")));
		nodeTemplate1.setCapabilities(ImmutableMap.of("os", capability));

		LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
		availableNodeTemplate1.setTemplate(nodeTemplate1);

		LocationResources locationResources = new LocationResources();
		locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
		locationResources.setNodeTypes(ImmutableMap.of("tosca.nodes.Compute", computeNodeType));

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
		capabilityFilterConfiguration.put("tosca.nodes.Compute", matchingConfiguration);

		// When
		NodeTemplate wantedNodeTemplate = new NodeTemplate();
		wantedNodeTemplate.setType("tosca.nodes.Compute");
		Capability wantedCapability = new Capability();
		wantedCapability.setType("tosca.capabilities.OperatingSystem");
		wantedCapability.setProperties(ImmutableMap.of("architecture", new ScalarPropertyValue("power_pc")));
		wantedNodeTemplate.setCapabilities(ImmutableMap.of("os", wantedCapability));
		List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, computeNodeType,
				locationResources, capabilityFilterConfiguration);

		// Then
		assertThat(proposition).hasSize(0);
	}

	@Test
	public void abstract_template_should_be_matched_if_service_is_available() throws Exception {
		// Given
		NodeType mongoDBNodeType = new NodeType();
		mongoDBNodeType.setAbstract(false);
		mongoDBNodeType.setElementId("alien.service.MongoDB");
		mongoDBNodeType.setDerivedFrom(Arrays.asList("test.nodes.Service", "test.nodes.DB"));

		NodeTemplate nodeTemplate1 = new NodeTemplate();
		nodeTemplate1.setType("alien.service.MongoDB");

		LocationResourceTemplate availableNodeTemplate1 = new LocationResourceTemplate();
		availableNodeTemplate1.setTemplate(nodeTemplate1);
		availableNodeTemplate1.setService(true);

		LocationResources locationResources = new LocationResources();
		locationResources.setNodeTemplates(Arrays.asList(availableNodeTemplate1));
		locationResources.setNodeTypes(ImmutableMap.of("alien.service.MongoDB", mongoDBNodeType));

		Map<String, MatchingConfiguration> emptyMatchingConfigurations = new HashMap<>();

		// When
		NodeTemplate wantedNodeTemplate = new NodeTemplate();
		wantedNodeTemplate.setType("test.nodes.DB");
		List<LocationResourceTemplate> proposition = nodeMatcher.matchNode(wantedNodeTemplate, null,
				locationResources, emptyMatchingConfigurations);

		// Then
		assertThat(proposition).hasSize(1);
		assertThat(proposition.get(0).isService()).isTrue();

		// When
		wantedNodeTemplate.setType("alien.service.MongoDB");
		proposition = nodeMatcher.matchNode(wantedNodeTemplate, null,
				locationResources, emptyMatchingConfigurations);

		// Then
		assertThat(proposition).hasSize(1);
		assertThat(proposition.get(0).isService()).isTrue();

	}
}