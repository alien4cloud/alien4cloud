package org.alien4cloud.tosca.catalog.index;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.dao.IESMetaPropertiesSearchContextBuilder;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.metaproperty.MPSearchContextBuilderMock;
import alien4cloud.model.common.Tag;
import com.google.common.collect.Lists;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import alien4cloud.images.ImageDAO;
import alien4cloud.utils.AlienYamlPropertiesFactoryBeanFactory;

/**
 * Simple test for tosca type indexer service.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@ActiveProfiles("ToscaTypeIndexerServiceTest")
@DirtiesContext
public class ToscaTypeIndexerServiceTest {
    @Profile("ToscaTypeIndexerServiceTest")
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
        public static ToscaTypeIndexerService toscaTypeIndexerService() {
            return new ToscaTypeIndexerService();
        }

        @Bean
        public static ImageDAO getImageDao() {
            return new ImageDAO();
        }

        @Bean
        public static IESMetaPropertiesSearchContextBuilder getIESMetaPropertiesSearchContextBuilder() {
            return new MPSearchContextBuilderMock();
        }

    }

    @Inject
    private ToscaTypeIndexerService toscaTypeIndexerService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    @Test
    public void test() throws Exception {
        alienDao.delete(NodeType.class, QueryBuilders.matchAllQuery());
        Method method = toscaTypeIndexerService.getClass().getDeclaredMethod("hasElementWithTag", Class.class, String.class, String.class);
        method.setAccessible(true);
        boolean hasElementWithTag = (boolean) method.invoke(toscaTypeIndexerService, NodeType.class, "icon", "my_icon");
        Assert.assertFalse(hasElementWithTag);

        NodeType nodeType = new NodeType();
        nodeType.setElementId("element");
        nodeType.setArchiveName("archive");
        nodeType.setArchiveVersion("1.0.0");
        nodeType.setTags(Lists.newArrayList(new Tag("icon", "another_icon")));
        alienDao.save(nodeType);

        hasElementWithTag = (boolean) method.invoke(toscaTypeIndexerService, NodeType.class, "icon", "my_icon");
        Assert.assertFalse(hasElementWithTag);

        nodeType = new NodeType();
        nodeType.setElementId("otherelement");
        nodeType.setArchiveName("archive");
        nodeType.setArchiveVersion("1.0.0");
        nodeType.setTags(Lists.newArrayList(new Tag("icon", "my_icon")));
        alienDao.save(nodeType);

        hasElementWithTag = (boolean) method.invoke(toscaTypeIndexerService, NodeType.class, "icon", "my_icon");
        Assert.assertTrue(hasElementWithTag);
    }
}