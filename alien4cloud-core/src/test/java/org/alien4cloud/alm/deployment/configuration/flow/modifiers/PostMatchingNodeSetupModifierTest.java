package org.alien4cloud.alm.deployment.configuration.flow.modifiers;

import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionLog;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodeCapabilitiesPropsOverride;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration.NodePropsOverride;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.ToscaParser;
import alien4cloud.tosca.topology.TemplateBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@ActiveProfiles("PostMatchingNodeSetupModifierTest")
public class PostMatchingNodeSetupModifierTest {

    @Profile("PostMatchingNodeSetupModifierTest")
    @Configuration
    @EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = { "alien4cloud.tosca.context", "alien4cloud.tosca.parser", "alien4cloud.paas.wf" })
    static class ContextConfiguration {
        @Bean
        public ICSARRepositorySearchService repositorySearchService() {
            return Mockito.mock(ICSARRepositorySearchService.class);
        }
    }

    @Resource
    private ICSARRepositorySearchService csarRepositorySearchService;
    @Resource
    private ToscaParser parser;

    @Test
    public void testPropertiesCleanup() throws ParsingException {
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getArchive("tosca-normative-types", "1.0.0-SNAPSHOT")).thenReturn(Mockito.mock(Csar.class));
        NodeType mockType = Mockito.mock(NodeType.class);
        Mockito.when(mockType.isAbstract()).thenReturn(true);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq("tosca.nodes.Root"), Mockito.any(Set.class)))
                .thenReturn(mockType);
        CapabilityType mockCapaType = Mockito.mock(CapabilityType.class);
        Mockito.when(mockCapaType.getElementId()).thenReturn("tosca.capabilities.Root");
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq("tosca.capabilities.Root"),
                Mockito.any(Set.class))).thenReturn(mockCapaType);

        ParsingResult<ArchiveRoot> parsingResult = parser
                .parseFile(Paths.get("../alien4cloud-test-common/src/test/resources/data/csars/matching-change-cleanup/tosca.yml"));

        for (Entry<String, CapabilityType> capabilityTypeEntry : parsingResult.getResult().getCapabilityTypes().entrySet()) {
            Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class), Mockito.eq(capabilityTypeEntry.getKey()),
                    Mockito.any(Set.class))).thenReturn(capabilityTypeEntry.getValue());
        }
        for (Entry<String, NodeType> nodeTypeEntry : parsingResult.getResult().getNodeTypes().entrySet()) {
            Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(NodeType.class), Mockito.eq(nodeTypeEntry.getKey()),
                    Mockito.any(Set.class))).thenReturn(nodeTypeEntry.getValue());
        }

        // Parse the archive that contains the test topology

        try {
            ToscaContext.init(Sets.newHashSet());

            PostMatchingNodeSetupModifier postMatchingNodeSetupModifier = new PostMatchingNodeSetupModifier();

            Topology topology = new Topology();
            topology.setNodeTemplates(Maps.newHashMap());
            topology.getNodeTemplates().put("my_node", TemplateBuilder
                    .buildNodeTemplate(parsingResult.getResult().getNodeTypes().get("org.alien4cloud.test.matching.nodes.LocationCustomImplOne")));

            // Configure the deployment config
            DeploymentMatchingConfiguration matchingConfiguration = new DeploymentMatchingConfiguration();

            matchingConfiguration.setMatchedLocationResources(Maps.newHashMap());
            matchingConfiguration.getMatchedLocationResources().put("my_node","a_location_resource");

            NodePropsOverride nodePropsOverride = new NodePropsOverride();
            nodePropsOverride.getProperties().put("common_property", new ScalarPropertyValue("p_val"));
            nodePropsOverride.getProperties().put("unique_prop", new ScalarPropertyValue("p_val"));
            nodePropsOverride.getProperties().put("type_variant_prop", new ScalarPropertyValue("p_val"));
            nodePropsOverride.getProperties().put("constraint_variant_prop", new ScalarPropertyValue("p_val"));

            NodeCapabilitiesPropsOverride nodeCapabilitiesPropsOverride = new NodeCapabilitiesPropsOverride();
            nodeCapabilitiesPropsOverride.getProperties().put("common_property", new ScalarPropertyValue("p_val"));
            nodeCapabilitiesPropsOverride.getProperties().put("unique_prop", new ScalarPropertyValue("p_val"));
            nodeCapabilitiesPropsOverride.getProperties().put("type_variant_prop", new ScalarPropertyValue("p_val"));
            nodeCapabilitiesPropsOverride.getProperties().put("constraint_variant_prop", new ScalarPropertyValue("p_val"));
            nodePropsOverride.getCapabilities().put("my_capability", nodeCapabilitiesPropsOverride);

            matchingConfiguration.setMatchedNodesConfiguration(Maps.newHashMap());
            matchingConfiguration.getMatchedNodesConfiguration().put("my_node", nodePropsOverride);

            FlowExecutionContext mockContext = Mockito.mock(FlowExecutionContext.class);
            Mockito.when(mockContext.log()).thenReturn(Mockito.mock(FlowExecutionLog.class));
            Mockito.when(mockContext.getConfiguration(DeploymentMatchingConfiguration.class, AbstractPostMatchingSetupModifier.class.getSimpleName()))
                    .thenReturn(Optional.of(matchingConfiguration));

            // All properties resources should be remain as matched type is compliant
            postMatchingNodeSetupModifier.process(topology, mockContext);
            Assert.assertEquals(4, nodePropsOverride.getProperties().size());
            Assert.assertEquals(4, nodePropsOverride.getCapabilities().get("my_capability").getProperties().size());
            // Change the type and check that properties are cleared
            topology.getNodeTemplates().clear();
            topology.getNodeTemplates().put("my_node", TemplateBuilder
                    .buildNodeTemplate(parsingResult.getResult().getNodeTypes().get("org.alien4cloud.test.matching.nodes.LocationCustomImplTwo")));
            postMatchingNodeSetupModifier.process(topology, mockContext);
            Assert.assertEquals(1, nodePropsOverride.getProperties().size());
            Assert.assertEquals(1, nodePropsOverride.getCapabilities().get("my_capability").getProperties().size());
        } finally {
            ToscaContext.destroy();
        }
    }
}
