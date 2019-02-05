package org.alien4cloud.alm.service;

import java.io.IOException;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.dao.IESMetaPropertiesSearchContextBuilder;
import alien4cloud.metaproperty.MPSearchContextBuilderMock;
import alien4cloud.orchestrators.locations.events.AfterLocationDeleted;
import org.alien4cloud.tosca.catalog.events.ArchiveUsageRequestEvent;
import org.alien4cloud.tosca.catalog.index.ToscaTypeSearchService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.topology.validation.TopologyPropertiesValidationService;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;
import alien4cloud.utils.services.PropertyService;

/**
 * Unit tests for service resource service.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("ServiceResourceServiceTest")
@DirtiesContext
public class ServiceResourceServiceTest {
    @Profile("ServiceResourceServiceTest")
    @Configuration
    @EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class })
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @ComponentScan(basePackages = { "org.elasticsearch.mapping", "alien4cloud.dao" })
    static class ContextConfiguration {
        @Bean(name = { "alienconfig", "elasticsearchConfig" })
        public static YamlPropertiesFactoryBean alienConfig(ResourceLoader resourceLoader) throws IOException {
            return AlienYamlPropertiesFactoryBeanFactory.get(resourceLoader);
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer properties(YamlPropertiesFactoryBean yamlPropertiesFactoryBean) {
            PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            propertySourcesPlaceholderConfigurer.setProperties(yamlPropertiesFactoryBean.getObject());
            return propertySourcesPlaceholderConfigurer;
        }

        @Bean
        public static ToscaTypeSearchService toscaTypeSearchService() {
            return new ToscaTypeSearchService();
        }

        @Bean
        public static PropertyService propertyService() {
            return new PropertyService();
        }

        @Bean
        public static TopologyPropertiesValidationService topologyPropertiesValidationService() {
            return new TopologyPropertiesValidationService();
        }

        @Bean
        public static NodeInstanceService nodeInstanceService() {
            return Mockito.mock(NodeInstanceService.class);
        }

        @Bean
        public static ServiceResourceService serviceResourceService() {
            return new ServiceResourceService();
        }

        @Bean
        public static IESMetaPropertiesSearchContextBuilder getIESMetaPropertiesSearchContextBuilder() {
            return new MPSearchContextBuilderMock();
        }
    }

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private ServiceResourceService serviceResourceService;

    @Test
    public void testGetByNodeTypes() {
        ServiceResource serviceResource = new ServiceResource();
        serviceResource.setId("service1");
        serviceResource.setNodeInstance(new NodeInstance());
        serviceResource.getNodeInstance().setTypeVersion("1.0.0-SNAPSHOT");
        serviceResource.getNodeInstance().setNodeTemplate(new NodeTemplate());
        serviceResource.getNodeInstance().getNodeTemplate().setType("org.alien4cloud.nodes.MyType");

        alienDao.save(serviceResource);

        ServiceResource[] services = serviceResourceService.getByNodeTypes("org.alien4cloud.nodes.MyType", "1.0.0-SNAPSHOT");

        Assert.assertNotNull(services);
        Assert.assertEquals(1, services.length);

        services = serviceResourceService.getByNodeTypes("org.alien4cloud.nodes.UnusedType", "1.0.0-SNAPSHOT");

        Assert.assertNotNull(services);
        Assert.assertEquals(0, services.length);

        services = serviceResourceService.getByNodeTypes("org.alien4cloud.nodes.MyType", "1.0.0");

        Assert.assertNotNull(services);
        Assert.assertEquals(0, services.length);
    }

    @Test
    public void testHandleLocationDeleted() {
        ServiceResource serviceResource = new ServiceResource();
        serviceResource.setId("service1");
        serviceResource.setLocationIds(new String[] { "location1", "location2" });

        alienDao.save(serviceResource);

        serviceResourceService.handleLocationDeleted(new AfterLocationDeleted(this, "location3"));

        serviceResource = serviceResourceService.getOrFail("service1");
        Assert.assertArrayEquals(new String[] { "location1", "location2" }, serviceResource.getLocationIds());

        serviceResourceService.handleLocationDeleted(new AfterLocationDeleted(this, "location1"));

        serviceResource = serviceResourceService.getOrFail("service1");
        Assert.assertArrayEquals(new String[] { "location2" }, serviceResource.getLocationIds());
    }

    @Test
    public void testReportArchiveUsage() {
        ServiceResource serviceResource = new ServiceResource();
        serviceResource.setId("service1");
        serviceResource.setName("service name 1");
        serviceResource.setDependency(new CSARDependency("org.alien4cloud.archives:my-archive", "1.0.0-SNAPSHOT"));

        alienDao.save(serviceResource);

        ArchiveUsageRequestEvent archiveUsageRequestEvent = new ArchiveUsageRequestEvent(this, "org.alien4cloud.archives:my-archive", "1.0.0-SNAPSHOT");
        serviceResourceService.reportArchiveUsage(archiveUsageRequestEvent);
        Assert.assertEquals(1, archiveUsageRequestEvent.getUsages().size());
        Assert.assertEquals("service1", archiveUsageRequestEvent.getUsages().get(0).getResourceId());
        Assert.assertEquals("serviceresource", archiveUsageRequestEvent.getUsages().get(0).getResourceType());
        Assert.assertEquals("service name 1", archiveUsageRequestEvent.getUsages().get(0).getResourceName());

        archiveUsageRequestEvent = new ArchiveUsageRequestEvent(this, "org.alien4cloud.archives:other-archive", "1.0.0-SNAPSHOT");
        serviceResourceService.reportArchiveUsage(archiveUsageRequestEvent);
        Assert.assertEquals(0, archiveUsageRequestEvent.getUsages().size());

        archiveUsageRequestEvent = new ArchiveUsageRequestEvent(this, "org.alien4cloud.archives:my-archive", "1.0.1-SNAPSHOT");
        serviceResourceService.reportArchiveUsage(archiveUsageRequestEvent);
        Assert.assertEquals(0, archiveUsageRequestEvent.getUsages().size());
    }
}