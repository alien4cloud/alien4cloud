package org.alien4cloud.tosca.utils;

import static org.alien4cloud.tosca.utils.NodeTemplateUtils.getCapabilityByType;
import static org.alien4cloud.tosca.utils.NodeTemplateUtils.getCapabilityByTypeOrFail;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.exception.NotFoundException;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.assertj.core.util.Maps;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.tosca.context.ToscaContextualAspect;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("org.alien4cloud.tosca.utils.NodeTemplateUtilsTest")
@DirtiesContext
public class NodeTemplateUtilsTest {
    @Configuration
    @Profile("org.alien4cloud.tosca.utils.NodeTemplateUtilsTest")
    @EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = { "alien4cloud.tosca.context" })
    static class ContextConfiguration {
        @Bean
        public ICSARRepositorySearchService csarRepositorySearchService() {
            return Mockito.mock(ICSARRepositorySearchService.class);
        }

        @Bean
        public ToscaContextualAspect toscaContextualAspect() {
            return new ToscaContextualAspect();
        }
    }

    @Resource
    private ICSARRepositorySearchService csarRepositorySearchService;
    @Resource
    private ToscaContextualAspect toscaContextualAspect;

    @Test
    public void getCapabilityByTypeTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.alien4cloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability type exactly equals then no tosca context and request is required
        Capability capability = getCapabilityByType(nodeTemplate, "org.alien4cloud.capabilities.SampleCapability");
        assertSame(nodeCapability, capability);

        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.alien4cloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.alien4cloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.alien4cloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByType(nodeTemplate, "org.alien4cloud.capabilities.TestCapability"), false,
                Sets.newHashSet(new CSARDependency("org.alien4cloud.testArchive", "1.0.0-SNAPSHOT")));
        assertSame(nodeCapability, capability);
    }

    @Test
    public void getMissingCapabilityByTypeTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.alien4cloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.alien4cloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.alien4cloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.alien4cloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        Capability capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByType(nodeTemplate, "org.alien4cloud.capabilities.Unknown"), false,
                Sets.newHashSet(new CSARDependency("org.alien4cloud.testArchive", "1.0.0-SNAPSHOT")));
        assertNull(capability);
    }

    @Test
    public void getCapabilityByTypeOrFailTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.alien4cloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability type exactly equals then no tosca context and request is required
        Capability capability = getCapabilityByTypeOrFail(nodeTemplate, "org.alien4cloud.capabilities.SampleCapability");
        assertSame(nodeCapability, capability);

        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.alien4cloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.alien4cloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.alien4cloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByTypeOrFail(nodeTemplate, "org.alien4cloud.capabilities.TestCapability"),
                false, Sets.newHashSet(new CSARDependency("org.alien4cloud.testArchive", "1.0.0-SNAPSHOT")));
        assertSame(nodeCapability, capability);
    }

    @Test(expected = NotFoundException.class)
    public void getMissingCapabilityByTypeOrFailTest() {
        NodeTemplate nodeTemplate = new NodeTemplate();
        Capability nodeCapability = new Capability("org.alien4cloud.capabilities.SampleCapability", null);
        nodeTemplate.setCapabilities(Maps.newHashMap("test", nodeCapability));
        // if the capability derives from parent type then a TOSCA context and query is required to fetch the type.
        CapabilityType capabilityType = new CapabilityType();
        capabilityType.setElementId("org.alien4cloud.capabilities.SampleCapability");
        capabilityType.setDerivedFrom(Lists.newArrayList("org.alien4cloud.capabilities.TestCapability"));
        Mockito.reset(csarRepositorySearchService);
        Mockito.when(csarRepositorySearchService.getElementInDependencies(Mockito.eq(CapabilityType.class),
                Mockito.eq("org.alien4cloud.capabilities.SampleCapability"), Mockito.any(Set.class))).thenReturn(capabilityType);

        Capability capability = toscaContextualAspect.execInToscaContext(() -> getCapabilityByTypeOrFail(nodeTemplate, "org.alien4cloud.capabilities.Unknown"),
                false, Sets.newHashSet(new CSARDependency("org.alien4cloud.testArchive", "1.0.0-SNAPSHOT")));
        assertNull(capability);
    }
}
