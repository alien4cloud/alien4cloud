package alien4cloud.it;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.yaml.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

import alien4cloud.it.exception.ITException;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.rest.utils.RestClient;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import cucumber.runtime.io.ClasspathResourceLoader;

/**
 * In order to communicate between different step definitions
 *
 * @author mkv
 *
 */
@Slf4j
public class Context {
    public static final String HOST = "localhost";

    public static final int PORT = 8088;

    public static final String CONTEXT_PATH = "";

    public static final String WEB_SOCKET_END_POINT = "/rest/alienEndPoint";

    /** Alien's current version. */
    public static final String VERSION;

    private static final Context INSTANCE = new Context();

    private static final Client ES_CLIENT_INSTANCE;

    private static final RestClient REST_CLIENT_INSTANCE;

    private static final ObjectMapper JSON_MAPPER;

    static {
        JSON_MAPPER = new ObjectMapper();
        Settings settings = ImmutableSettings.settingsBuilder().put("discovery.zen.ping.multicast.enabled", false)
                .put("discovery.zen.ping.unicast.hosts", "localhost").put("discovery.zen.ping.unicast.enabled", true).build();
        ES_CLIENT_INSTANCE = NodeBuilder.nodeBuilder().client(true).clusterName("escluster").local(false).settings(settings).node().client();
        REST_CLIENT_INSTANCE = new RestClient("http://" + HOST + ":" + PORT + CONTEXT_PATH);
        YamlPropertiesFactoryBean propertiesFactoryBean = new YamlPropertiesFactoryBean();
        propertiesFactoryBean.setResources(new Resource[] { new ClassPathResource("version.yml") });
        Properties properties = propertiesFactoryBean.getObject();
        VERSION = properties.getProperty("version");
    }

    public static Client getEsClientInstance() {
        return ES_CLIENT_INSTANCE;
    }

    public static RestClient getRestClientInstance() {
        return REST_CLIENT_INSTANCE;
    }

    public static Context getInstance() {
        return INSTANCE;
    }

    public ObjectMapper getJsonMapper() {
        return JSON_MAPPER;
    }

    private final TestPropertyPlaceholderConfigurer appProps;

    private ThreadLocal<String> restResponseLocal;

    private ThreadLocal<String> csarLocal;

    private ThreadLocal<Set<String>> componentsIdsLocal;

    private ThreadLocal<String> topologyIdLocal;

    private ThreadLocal<String> csarIdLocal;

    private ThreadLocal<TopologyTemplate> topologyTemplate;

    private ThreadLocal<Application> applicationLocal;

    private ThreadLocal<Map<String, String>> applicationInfos;

    private ThreadLocal<Map<String, String>> cloudInfos;

    private ThreadLocal<String> topologyCloudInfos;

    private ThreadLocal<Map<String, String>> deployApplicationProperties;

    private ThreadLocal<Map<String, MetaPropConfiguration>> configurationTags;

    private ThreadLocal<String> topologyDeploymentId;

    private ThreadLocal<Map<String, String>> groupIdToGroupNameMapping;

    private ThreadLocal<Map<String, String>> cloudImageNameToCloudImageIdMapping;

    private ThreadLocal<Map<String, String>> applicationVersionNameToApplicationVersionIdMapping;

    private ThreadLocal<Map<String, String>> environmentInfos;

    private Context() {
        restResponseLocal = new ThreadLocal<String>();
        csarLocal = new ThreadLocal<String>();
        componentsIdsLocal = new ThreadLocal<Set<String>>();
        topologyIdLocal = new ThreadLocal<String>();
        csarIdLocal = new ThreadLocal<String>();
        applicationLocal = new ThreadLocal<Application>();
        topologyTemplate = new ThreadLocal<TopologyTemplate>();
        cloudInfos = new ThreadLocal<Map<String, String>>();
        topologyCloudInfos = new ThreadLocal<String>();
        deployApplicationProperties = new ThreadLocal<Map<String, String>>();
        configurationTags = new ThreadLocal<Map<String, MetaPropConfiguration>>();
        topologyDeploymentId = new ThreadLocal<String>();
        groupIdToGroupNameMapping = new ThreadLocal<Map<String, String>>();
        groupIdToGroupNameMapping.set(new HashMap<String, String>());
        cloudImageNameToCloudImageIdMapping = new ThreadLocal<>();
        cloudImageNameToCloudImageIdMapping.set(new HashMap<String, String>());
        applicationVersionNameToApplicationVersionIdMapping = new ThreadLocal<>();
        applicationVersionNameToApplicationVersionIdMapping.set(new HashMap<String, String>());
        environmentInfos = new ThreadLocal<Map<String, String>>();
        applicationInfos = new ThreadLocal<Map<String, String>>();
        ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(Thread.currentThread().getContextClassLoader());
        Iterable<cucumber.runtime.io.Resource> properties = classpathResourceLoader.resources("", "alien4cloud-config.yml");
        List<Resource> resources = Lists.newArrayList();
        for (cucumber.runtime.io.Resource property : properties) {
            try {
                resources.add(new InputStreamResource(property.getInputStream()));
            } catch (IOException e) {
                throw new ITException("Could not load properties file at [" + property.getPath() + "]", e);
            }
        }
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resources.toArray(new Resource[resources.size()]));
        appProps = new TestPropertyPlaceholderConfigurer();
        appProps.setProperties(factory.getObject());
    }

    private static class TestPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

        private PropertyPlaceholderHelper.PlaceholderResolver resolver;

        private Properties properties;

        private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper(placeholderPrefix, placeholderSuffix, valueSeparator,
                ignoreUnresolvablePlaceholders);

        @SneakyThrows
        public String getProperty(String key) {
            if (resolver == null) {
                properties = mergeProperties();
                convertProperties(properties);
                resolver = new PropertyPlaceholderConfigurerResolver(properties);
            }
            String value = this.helper.replacePlaceholders(resolvePlaceholder(key, properties, SYSTEM_PROPERTIES_MODE_FALLBACK), this.resolver);
            return (value.equals(nullValue) ? null : value);
        }

        private class PropertyPlaceholderConfigurerResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

            private final Properties props;

            private PropertyPlaceholderConfigurerResolver(Properties props) {
                this.props = props;
            }

            @Override
            public String resolvePlaceholder(String placeholderName) {
                return TestPropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, props, SYSTEM_PROPERTIES_MODE_FALLBACK);
            }
        }
    }

    public void registerGroupId(String groupName, String groupId) {
        groupIdToGroupNameMapping.get().put(groupName, groupId);
    }

    public String getGroupId(String groupName) {
        return groupIdToGroupNameMapping.get().get(groupName);
    }

    public void registerCloudImageId(String cloudImageName, String cloudImageId) {
        cloudImageNameToCloudImageIdMapping.get().put(cloudImageName, cloudImageId);
    }

    public String getCloudImageId(String cloudImageName) {
        return cloudImageNameToCloudImageIdMapping.get().get(cloudImageName);
    }

    public void registerApplicationVersionId(String applicationVersionName, String applicationVersionId) {
        applicationVersionNameToApplicationVersionIdMapping.get().put(applicationVersionName, applicationVersionId);
    }

    public String getApplicationVersionId(String applicationVersionName) {
        return applicationVersionNameToApplicationVersionIdMapping.get().get(applicationVersionName);
    }

    public void registerRestResponse(String restResponse) {
        log.debug("Registering [" + restResponse + "] in the context");
        restResponseLocal.set(restResponse);
    }

    public String getRestResponse() {
        return restResponseLocal.get();
    }

    public String takeRestResponse() {
        String response = restResponseLocal.get();
        restResponseLocal.set(null);
        return response;
    }

    public void registerCSAR(String csarPath) {
        log.debug("Registering csar [" + csarPath + "] in the context");
        csarLocal.set(csarPath);
    }

    public String getCSAR() {
        String csar = csarLocal.get();
        csarLocal.set(null);
        return csar;
    }

    public String getAppProperty(String key) {
        return appProps.getProperty(key);
    }

    public Path getAlienPath() {
        return Paths.get(getAppProperty("directories.alien"));
    }

    public Path getRepositoryDirPath() {
        return Paths.get(getAlienPath() + "/" + getAppProperty("directories.csar_repository"));
    }

    public Path getUploadTempDirPath() {
        return Paths.get(getAlienPath() + "/" + getAppProperty("directories.upload_temp"));
    }

    public Path getPluginDirPath() {
        return Paths.get(getAlienPath() + "/" + getAppProperty("directories.plugins"));
    }

    public Path getArtifactDirPath() {
        return Paths.get(getAlienPath() + "/" + getAppProperty("directories.artifact_repository"));
    }

    public Path getTmpDirectory() {
        return Paths.get("target/tmp/");
    }

    public String getElasticSearchClusterName() {
        return getAppProperty("elasticSearch.clusterName");
    }

    public void clearComponentsIds() {
        componentsIdsLocal.set(null);
    }

    public Set<String> getComponentsIds() {
        return componentsIdsLocal.get();
    }

    public Set<String> takeComponentsIds() {
        Set<String> componentsIds = componentsIdsLocal.get();
        clearComponentsIds();
        return componentsIds;
    }

    public void registerComponentId(String componentid) {
        if (componentsIdsLocal.get() == null) {
            componentsIdsLocal.set(new HashSet<String>());
        }
        log.debug("Registering componentID [" + componentid + "] in the context");
        componentsIdsLocal.get().add(componentid);
    }

    public String getComponentId(String componentid) {
        if (componentsIdsLocal.get() != null && componentsIdsLocal.get().contains(componentid)) {
            return componentid;
        }
        return null;
    }

    public String takeComponentId(String componentid) {
        if (componentsIdsLocal.get() != null && componentsIdsLocal.get().contains(componentid)) {
            componentsIdsLocal.get().remove(componentid);
            return componentid;
        }
        return null;
    }

    public String getComponentId(int position) {
        if (componentsIdsLocal.get() != null && componentsIdsLocal.get().size() >= (position - 1)) {
            int index = 0;
            for (String componentID : componentsIdsLocal.get()) {
                if (index == position) {
                    return componentID;
                }
                index++;
            }
        }
        return null;
    }

    public String takeComponentId(int position) {
        if (componentsIdsLocal.get() != null && componentsIdsLocal.get().size() >= (position - 1)) {
            int index = 0;
            for (String componentID : componentsIdsLocal.get()) {
                if (index == position) {
                    componentsIdsLocal.get().remove(componentID);
                    return componentID;
                }
                index++;
            }
        }
        return null;
    }

    public void registerTopologyId(String topologyId) {
        log.debug("Registering topology Id [" + topologyId + "] in the context");
        topologyIdLocal.set(topologyId);
    }

    public String getTopologyId() {
        return topologyIdLocal.get();
    }

    public String takeTopologyId() {
        String topoId = topologyIdLocal.get();
        topologyIdLocal.set(null);
        return topoId;
    }

    public void registerCsarId(String csarId) {
        log.debug("Registering csarId Id [" + csarId + "] in the context");
        csarIdLocal.set(csarId);
    }

    public String getCsarId() {
        return csarIdLocal.get();
    }

    public String takeCsarId() {
        String csarId = csarIdLocal.get();
        csarIdLocal.set(null);
        return csarId;
    }

    public void registerApplication(Application application) {
        log.debug("Registering application [" + application.getId() + "] in the context");
        applicationLocal.set(application);
    }

    public Application getApplication() {
        return applicationLocal.get();
    }

    public Application takeApplication() {
        Application app = applicationLocal.get();
        applicationLocal.set(null);
        return app;
    }

    public TopologyTemplate getTopologyTemplate() {
        return topologyTemplate.get();
    }

    public TopologyTemplate takeTopologyTemplate() {
        TopologyTemplate ttId = topologyTemplate.get();
        topologyTemplate.set(null);
        return ttId;
    }

    public void registerTopologyTemplate(TopologyTemplate topologTemplate) {
        log.debug("Registering topology template [" + topologTemplate + "] in the context");
        topologyTemplate.set(topologTemplate);
    }

    public void registerCloud(String cloudId, String cloudName) {
        if (cloudInfos.get() != null) {
            cloudInfos.get().put(cloudName, cloudId);
            return;
        }
        cloudInfos.set(MapUtil.newHashMap(new String[] { cloudName }, new String[] { cloudId }));
    }

    public void unregisterCloud(String cloudName) {
        cloudInfos.get().remove(cloudName);
    }

    public String getCloudId(String cloudName) {
        return cloudInfos.get().get(cloudName);
    }

    public void registerCloudForTopology(String cloudId) {
        topologyCloudInfos.set(cloudId);
    }

    public String getCloudForTopology() {
        return topologyCloudInfos.get();
    }

    public void registerDeployApplicationProperties(Map<String, String> deployApplicationProperties) {
        this.deployApplicationProperties.set(deployApplicationProperties);
    }

    public Map<String, String> getDeployApplicationProperties() {
        return deployApplicationProperties.get();
    }

    public void registerConfigurationTag(String configurationTagName, MetaPropConfiguration tagConfiguration) {
        if (configurationTags.get() != null) {
            configurationTags.get().put(configurationTagName, tagConfiguration);
            return;
        }
        configurationTags.set(MapUtil.newHashMap(new String[] { configurationTagName }, new MetaPropConfiguration[] { tagConfiguration }));
    }

    public MetaPropConfiguration getConfigurationTag(String configurationTagName) {
        return configurationTags.get().get(configurationTagName);
    }

    public MetaPropConfiguration takeConfigurationTag(String configurationTagName) {
        return configurationTags.get().get(configurationTagName);
    }

    public Map<String, MetaPropConfiguration> getConfigurationTags() {
        return configurationTags.get();
    }

    public void registerTopologyDeploymentId(String topoDeploymentId) {
        topologyDeploymentId.set(topoDeploymentId);
    }

    public String getTopologyDeploymentId() {
        return topologyDeploymentId.get();
    }

    public void registerApplicationEnvironmentId(String applicationEnvironmentName, String applicationEnvironmentId) {
        if (this.environmentInfos.get() != null) {
            this.environmentInfos.get().put(applicationEnvironmentName, applicationEnvironmentId);
            return;
        }
        this.environmentInfos.set(MapUtil.newHashMap(new String[] { applicationEnvironmentName }, new String[] { applicationEnvironmentId }));
    }

    public String getApplicationEnvironmentId(String applicationEnvironmentName) {
        return this.environmentInfos.get().get(applicationEnvironmentName);
    }

    public void registerApplicationId(String applicationName, String applicationId) {
        if (this.applicationInfos.get() != null) {
            this.applicationInfos.get().put(applicationName, applicationId);
            return;
        }
        this.applicationInfos.set(MapUtil.newHashMap(new String[] { applicationName }, new String[] { applicationId }));
    }

    public String getApplicationId(String applicationName) {
        return this.applicationInfos.get().get(applicationName);
    }
}
