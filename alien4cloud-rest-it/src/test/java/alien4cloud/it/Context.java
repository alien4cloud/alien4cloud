package alien4cloud.it;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.PropertyPlaceholderHelper;

import alien4cloud.it.exception.ITException;
import alien4cloud.json.deserializer.PropertyValueDeserializer;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.templates.TopologyTemplate;
import alien4cloud.rest.utils.RestClient;
import alien4cloud.rest.utils.RestMapper;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
        JSON_MAPPER = new RestMapper();
        SimpleModule module = new SimpleModule("PropDeser", new Version(1, 0, 0, null));
        module.addDeserializer(AbstractPropertyValue.class, new PropertyValueDeserializer());
        JSON_MAPPER.registerModule(module);

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

    public static ObjectMapper getJsonMapper() {
        return JSON_MAPPER;
    }

    private final TestPropertyPlaceholderConfigurer appProps;

    private String restResponseLocal;

    private String csarLocal;

    private Set<String> componentsIdsLocal;

    private String topologyIdLocal;

    private String csarIdLocal;

    private TopologyTemplate topologyTemplate;

    private String topologyTemplateVersionId;

    private EvaluationContext spelEvaluationContext;

    private Application applicationLocal;

    private Map<String, String> applicationInfos;
    
    private Map<String,String> csarGitId;

    private Map<String, String> cloudInfos;

    private String topologyCloudInfos;

    private Map<String, String> deployApplicationProperties;

    private Map<String, MetaPropConfiguration> configurationTags;

    private String topologyDeploymentId;

    private Map<String, String> groupIdToGroupNameMapping = Maps.newHashMap();

    private Map<String, String> cloudImageNameToCloudImageIdMapping= Maps.newHashMap();

    private Map<String, String> applicationVersionNameToApplicationVersionIdMapping= Maps.newHashMap();

    private Map<String, Map<String, String>> environmentInfos;

    private Context() {
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
        if (resources.isEmpty()) {
            resources = Lists.<Resource> newArrayList(new ClassPathResource("alien4cloud-config.yml"));
        }
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
        groupIdToGroupNameMapping.put(groupName, groupId);
    }

    public String getGroupId(String groupName) {
        return groupIdToGroupNameMapping.get(groupName);
    }

    public void registerCloudImageId(String cloudImageName, String cloudImageId) {
        cloudImageNameToCloudImageIdMapping.put(cloudImageName, cloudImageId);
    }

    public String getCloudImageId(String cloudImageName) {
        return cloudImageNameToCloudImageIdMapping.get(cloudImageName);
    }

    public Map<String,String> getCsarGitId() {
        return csarGitId;
    }
    
    public void registerApplicationVersionId(String applicationVersionName, String applicationVersionId) {
        applicationVersionNameToApplicationVersionIdMapping.put(applicationVersionName, applicationVersionId);
    }

    public String getApplicationVersionId(String applicationVersionName) {
        return applicationVersionNameToApplicationVersionIdMapping.get(applicationVersionName);
    }

    public void registerRestResponse(String restResponse) {
        log.debug("Registering [" + restResponse + "] in the context");
        restResponseLocal = restResponse;
    }

    public String getRestResponse() {
        return restResponseLocal;
    }

    public String takeRestResponse() {
        String response = restResponseLocal;
        restResponseLocal = null;
        return response;
    }

    public void registerCSAR(String csarPath) {
        log.debug("Registering csar [" + csarPath + "] in the context");
        csarLocal = csarPath;
    }

    public String getCSAR() {
        String csar = csarLocal;
        csarLocal = null;
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
        componentsIdsLocal = null;
    }

    public Set<String> getComponentsIds() {
        return componentsIdsLocal;
    }

    public Set<String> takeComponentsIds() {
        Set<String> componentsIds = componentsIdsLocal;
        clearComponentsIds();
        return componentsIds;
    }

    public void registerComponentId(String componentid) {
        if (componentsIdsLocal == null) {
            componentsIdsLocal = new HashSet<String>();
        }
        log.debug("Registering componentID [" + componentid + "] in the context");
        componentsIdsLocal.add(componentid);
    }

    public String getComponentId(String componentid) {
        if (componentsIdsLocal != null && componentsIdsLocal.contains(componentid)) {
            return componentid;
        }
        return null;
    }

    public String takeComponentId(String componentid) {
        if (componentsIdsLocal != null && componentsIdsLocal.contains(componentid)) {
            componentsIdsLocal.remove(componentid);
            return componentid;
        }
        return null;
    }

    public String getComponentId(int position) {
        if (componentsIdsLocal != null && componentsIdsLocal.size() >= (position - 1)) {
            int index = 0;
            for (String componentID : componentsIdsLocal) {
                if (index == position) {
                    return componentID;
                }
                index++;
            }
        }
        return null;
    }

    public String takeComponentId(int position) {
        if (componentsIdsLocal != null && componentsIdsLocal.size() >= (position - 1)) {
            int index = 0;
            for (String componentID : componentsIdsLocal) {
                if (index == position) {
                    componentsIdsLocal.remove(componentID);
                    return componentID;
                }
                index++;
            }
        }
        return null;
    }

    public void registerTopologyId(String topologyId) {
        log.debug("Registering topology Id [" + topologyId + "] in the context");
        topologyIdLocal = topologyId;
    }

    public String getTopologyId() {
        return topologyIdLocal;
    }

    public String takeTopologyId() {
        String topoId = topologyIdLocal;
        topologyIdLocal = null;
        return topoId;
    }

    public void registerCsarId(String csarId) {
        log.debug("Registering csarId Id [" + csarId + "] in the context");
        csarIdLocal = csarId;
    }

    public String getCsarId() {
        return csarIdLocal;
    }

    public String takeCsarId() {
        String csarId = csarIdLocal;
        csarIdLocal = null;
        return csarId;
    }

    public void registerApplication(Application application) {
        log.debug("Registering application [" + application.getId() + "] in the context");
        applicationLocal = application;
    }

    public Application getApplication() {
        return applicationLocal;
    }

    public Application takeApplication() {
        Application app = applicationLocal;
        applicationLocal = null;
        return app;
    }

    public TopologyTemplate getTopologyTemplate() {
        return topologyTemplate;
    }

    public TopologyTemplate takeTopologyTemplate() {
        TopologyTemplate ttId = topologyTemplate;
        topologyTemplate = null;
        return ttId;
    }

    public void registerTopologyTemplate(TopologyTemplate topologTemplate) {
        log.debug("Registering topology template [" + topologTemplate + "] in the context");
        topologyTemplate = topologTemplate;
    }

    public EvaluationContext getSpelEvaluationContext() {
        return spelEvaluationContext;
    }

    public void buildEvaluationContext(Object object) {
        log.debug("Building evaluation context with object of class [" + object.getClass() + "] and keep it in the context");
        spelEvaluationContext = new StandardEvaluationContext(object);
    }

    public void registerCloud(String cloudId, String cloudName) {
        if (cloudInfos != null) {
            cloudInfos.put(cloudName, cloudId);
            return;
        }
        cloudInfos = MapUtil.newHashMap(new String[] { cloudName }, new String[] { cloudId });
    }

    public void unregisterCloud(String cloudName) {
        cloudInfos.remove(cloudName);
    }

    public String getCloudId(String cloudName) {
        return cloudInfos.get(cloudName);
    }

    public Collection<String> getCloudsIds() {
        if (cloudInfos != null) {
            return cloudInfos.values();
        } else {
            return Lists.newArrayList();
        }
    }

    public void registerCloudForTopology(String cloudId) {
        topologyCloudInfos = cloudId;
    }

    public void saveCsarGitId(String id,String url) {
        if (this.csarGitId != null) {
            this.csarGitId.put(id,url);
            return;
        }
        this.csarGitId = MapUtil.newHashMap(new String[] { id }, new String[] { url });
        csarGitId.put(id, url);
    }

    public String getCloudForTopology() {
        return topologyCloudInfos;
    }

    public void registerDeployApplicationProperties(Map<String, String> deployApplicationProperties) {
        this.deployApplicationProperties = deployApplicationProperties;
    }

    public Map<String, String> getDeployApplicationProperties() {
        return deployApplicationProperties;
    }

    public void registerConfigurationTag(String configurationTagName, MetaPropConfiguration tagConfiguration) {
        if (configurationTags != null) {
            configurationTags.put(configurationTagName, tagConfiguration);
            return;
        }
        configurationTags = MapUtil.newHashMap(new String[] { configurationTagName }, new MetaPropConfiguration[] { tagConfiguration });
    }

    public MetaPropConfiguration getConfigurationTag(String configurationTagName) {
        return configurationTags.get(configurationTagName);
    }

    public MetaPropConfiguration takeConfigurationTag(String configurationTagName) {
        return configurationTags.get(configurationTagName);
    }

    public Map<String, MetaPropConfiguration> getConfigurationTags() {
        return configurationTags;
    }

    public void registerTopologyDeploymentId(String topoDeploymentId) {
        topologyDeploymentId = topoDeploymentId;
    }

    public String getTopologyDeploymentId() {
        return topologyDeploymentId;
    }

    public void registerApplicationEnvironmentId(String applicationName, String applicationEnvironmentName, String applicationEnvironmentId) {
        Map<String, Map<String, String>> envsInfoMap = this.environmentInfos;
        if (envsInfoMap != null) {
            Map<String, String> envs = envsInfoMap.get(applicationName);
            if (envs == null) {
                envs = Maps.newHashMap();
            }
            envs.put(applicationEnvironmentName, applicationEnvironmentId);
            envsInfoMap.put(applicationName, envs);
            this.environmentInfos = envsInfoMap;
            return;

        }
        envsInfoMap = Maps.newHashMap();
        envsInfoMap.put(applicationName, MapUtil.newHashMap(new String[] { applicationEnvironmentName }, new String[] { applicationEnvironmentId }));
        this.environmentInfos = envsInfoMap;
    }

    public String getApplicationEnvironmentId(String applicationName, String applicationEnvironmentName) {
        return this.environmentInfos.get(applicationName).get(applicationEnvironmentName);
    }

    public Map<String, String> getAllEnvironmentForApplication(String applicationName) {
        return this.environmentInfos.get(applicationName);
    }

    public String getDefaultApplicationEnvironmentId(String applicationName) {
        return getApplicationEnvironmentId(applicationName, "Environment");
    }

    public void registerApplicationId(String applicationName, String applicationId) {
        if (this.applicationInfos != null) {
            this.applicationInfos.put(applicationName, applicationId);
            return;
        }
        this.applicationInfos = MapUtil.newHashMap(new String[] { applicationName }, new String[] { applicationId });
    }

    public String getApplicationId(String applicationName) {
        return this.applicationInfos.get(applicationName);
    }

    public void registerTopologyTemplateVersionId(String versionId) {
        topologyTemplateVersionId = versionId;
    }

    public String getTopologyTemplateVersionId() {
        return topologyTemplateVersionId;
    }
}
