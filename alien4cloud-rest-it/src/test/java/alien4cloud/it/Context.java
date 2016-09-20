package alien4cloud.it;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyConstraint;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.apache.commons.lang3.StringUtils;
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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.it.exception.ITException;
import alien4cloud.it.provider.util.AwsClient;
import alien4cloud.it.provider.util.OpenStackClient;
import alien4cloud.it.utils.TestUtils;
import alien4cloud.json.deserializer.*;
import alien4cloud.model.application.Application;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.rest.utils.RestClient;
import alien4cloud.rest.utils.RestMapper;
import alien4cloud.topology.task.AbstractTask;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.VersionUtil;
import cucumber.runtime.io.ClasspathResourceLoader;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * In order to communicate between different step definitions
 *
 * @author mkv
 *
 */
@Slf4j
public class Context {
    public static final String FASTCONNECT_NEXUS = "http://fastconnect.org/maven/service/local/artifact/maven/redirect?";

    public static final String GIT_URL_SUFFIX = ".git";

    public static final Path GIT_ARTIFACT_TARGET_PATH = Paths.get("target/git");

    public static final Path CSAR_TARGET_PATH = Paths.get("target/csars");

    public static final Path LOCAL_TEST_DATA_PATH = Paths.get("src/test/resources");

    public static final int SCP_PORT = 22;

    public static final String HOST = "localhost";

    public static final int PORT = 8088;

    public static final String CONTEXT_PATH = "";

    public static final String WEB_SOCKET_END_POINT = "/rest/v1/alienEndPoint";

    /** Alien's current version. */
    public static final String VERSION;

    private static final Context INSTANCE = new Context();

    private static Client ES_CLIENT_INSTANCE;

    private static RestClient REST_CLIENT_INSTANCE;

    private static ObjectMapper JSON_MAPPER;

    private static String ES_HOST = "localhost";

    private static String ES_CLUSTER = "escluster";

    static {
        YamlPropertiesFactoryBean propertiesFactoryBean = new YamlPropertiesFactoryBean();
        propertiesFactoryBean.setResources(new Resource[] { new ClassPathResource("version.yml") });
        Properties properties = propertiesFactoryBean.getObject();
        VERSION = properties.getProperty("version");

        ClassPathResource esClasspathResource = new ClassPathResource("es.yml");
        if (esClasspathResource.exists()) {
            propertiesFactoryBean = new YamlPropertiesFactoryBean();
            propertiesFactoryBean.setResources(new Resource[] { esClasspathResource });
            properties = propertiesFactoryBean.getObject();
            if (properties.containsKey("host")) {
                ES_HOST = properties.getProperty("host");
            }
            if (properties.containsKey("cluster")) {
                ES_CLUSTER = properties.getProperty("cluster");
            }
        }
    }

    public static Client getEsClientInstance() {
        if (ES_CLIENT_INSTANCE == null) {
            Settings settings = ImmutableSettings.settingsBuilder().put("discovery.zen.ping.multicast.enabled", false)
                    .put("discovery.zen.ping.unicast.hosts", ES_HOST).put("discovery.zen.ping.unicast.enabled", true).build();
            ES_CLIENT_INSTANCE = NodeBuilder.nodeBuilder().client(true).clusterName(ES_CLUSTER).local(false).settings(settings).node().client();
        }
        return ES_CLIENT_INSTANCE;
    }

    public static RestClient getRestClientInstance() {
        if (REST_CLIENT_INSTANCE == null) {
            REST_CLIENT_INSTANCE = new RestClient("http://" + HOST + ":" + PORT + CONTEXT_PATH);
        }
        return REST_CLIENT_INSTANCE;
    }

    public static Context getInstance() {
        return INSTANCE;
    }

    public static ObjectMapper getJsonMapper() {
        if (JSON_MAPPER == null) {
            JSON_MAPPER = new RestMapper();
            SimpleModule module = new SimpleModule("PropDeser", new Version(1, 0, 0, null, null, null));
            module.addDeserializer(AbstractPropertyValue.class, new PropertyValueDeserializer());
            module.addDeserializer(IValue.class, new AttributeDeserializer());
            try {
                module.addDeserializer(PropertyConstraint.class, new PropertyConstraintDeserializer());
            } catch (ClassNotFoundException | IOException | IntrospectionException e) {
                log.error("Unable to initialize test context.");
            }
            JSON_MAPPER.registerModule(module);

            // task deserializers
            module = new SimpleModule("taskDeser", new Version(1, 0, 0, null, null, null));
            module.addDeserializer(AbstractTask.class, new TaskDeserializer());
            module.addDeserializer(AbstractInheritableToscaType.class, new TaskIndexedInheritableToscaElementDeserializer());
            JSON_MAPPER.registerModule(module);

        }
        return JSON_MAPPER;
    }

    private final TestPropertyPlaceholderConfigurer appProps;

    private String restResponseLocal;

    private String csarLocal;

    private Set<String> componentsIdsLocal;

    private String topologyIdLocal;

    private String csarIdLocal;
    /*templateName -> templateVersion -> topologyId*/
    private Map<String, Map<String, String>> topologyTemplateId = Maps.newHashMap();

    private EvaluationContext spelEvaluationContext;

    private Application applicationLocal;

    private Map<String, String> applicationInfos;

    private Map<String, String> orchestratorIds;

    private Map<String, Map<String, String>> orchestratorLocationIds;

    /* orchestratorId -> { locationID -> { resourceName -> resourceId } } */
    private Map<String, Map<String, Map<String, String>>> orchestratorLocationResourceIds;

    private String topologyCloudInfos;

    private Map<String, String> preRegisteredOrchestratorProperties;

    private Map<String, Object> orchestratorConfiguration;

    private Map<String, MetaPropConfiguration> configurationTags;

    private String topologyDeploymentId;

    private Map<String, String> groupIdToGroupNameMapping = Maps.newHashMap();

    private Map<String, String> cloudImageNameToCloudImageIdMapping = Maps.newHashMap();

    private Map<String, String> applicationVersionNameToApplicationVersionIdMapping = Maps.newHashMap();

    private Map<String, Map<String, String>> environmentInfos;

    private OpenStackClient openStackClient;

    private AwsClient awsClient;

    private String currentWorkflowName;

    private String csarGitRepositoryId;

    private String currentExternalId;

    private Map<String, String> stringContent = new HashMap<String, String>();

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
        this.appProps = new TestPropertyPlaceholderConfigurer();
        this.appProps.setProperties(factory.getObject());
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
            String value = this.helper.replacePlaceholders(resolvePlaceholder(key, properties, SYSTEM_PROPERTIES_MODE_OVERRIDE), this.resolver);
            return (value.equals(nullValue) ? null : value);
        }

        private class PropertyPlaceholderConfigurerResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

            private final Properties props;

            private PropertyPlaceholderConfigurerResolver(Properties props) {
                this.props = props;
            }

            @Override
            public String resolvePlaceholder(String placeholderName) {
                return TestPropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, props, SYSTEM_PROPERTIES_MODE_OVERRIDE);
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
        return Paths.get(getAlienPath() + "/plugins");
    }

    public Path getWorkPath() {
        return Paths.get(getAlienPath() + "/work");
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

    public EvaluationContext getSpelEvaluationContext() {
        return spelEvaluationContext;
    }

    public void buildEvaluationContext(Object object) {
        log.debug("Building evaluation context with object of class [" + object.getClass() + "] and keep it in the context");
        spelEvaluationContext = new StandardEvaluationContext(object);
    }

    public void registerOrchestrator(String orchestratorId, String orchestratorName) {
        if (orchestratorIds != null) {
            orchestratorIds.put(orchestratorName, orchestratorId);
            return;
        }
        orchestratorIds = MapUtil.newHashMap(new String[] { orchestratorName }, new String[] { orchestratorId });
    }

    public void unregisterOrchestrator(String orchestratorName) {
        orchestratorIds.remove(orchestratorName);
    }

    public String getOrchestratorId(String orchestratorName) {
        return orchestratorIds.get(orchestratorName);
    }

    public Collection<String> getOrchestratorIds() {
        if (orchestratorIds != null) {
            return orchestratorIds.values();
        } else {
            return Lists.newArrayList();
        }
    }

    public void registerOrchestratorProperties(Map<String, String> deployApplicationProperties) {
        this.preRegisteredOrchestratorProperties = deployApplicationProperties;
    }

    public Map<String, String> getPreRegisteredOrchestratorProperties() {
        return preRegisteredOrchestratorProperties;
    }

    public Map<String, String> takePreRegisteredOrchestratorProperties() {
        Map<String, String> tmp = preRegisteredOrchestratorProperties;
        preRegisteredOrchestratorProperties = null;
        return tmp;
    }

    public Map<String, Object> getOrchestratorConfiguration() {
        return orchestratorConfiguration;
    }

    public void setOrchestratorConfiguration(Map<String, Object> orchestratorConfiguration) {
        this.orchestratorConfiguration = orchestratorConfiguration;
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

    public void registerTopologyTemplateId(String topologyId) {
        String name = TestUtils.getNameFromId(topologyId);
        if (topologyTemplateId.get(name) == null) {
            topologyTemplateId.put(name, Maps.newHashMap());
        }
        topologyTemplateId.get(name).put(TestUtils.getVersionFromId(topologyId), topologyId);
    }

    public String getTopologyTemplateId(String name, String version) {
        if (StringUtils.isBlank(version)) {
            version = VersionUtil.DEFAULT_VERSION_NAME;
        }
        return (String) MapUtil.get(topologyTemplateId, Joiner.on(".").join(name, version).toString());
    }

    public OpenStackClient getOpenStackClient() {
        if (this.openStackClient == null) {
            this.openStackClient = new OpenStackClient(this.appProps.getProperty("openstack.user"), this.appProps.getProperty("openstack.password"),
                    this.appProps.getProperty("openstack.tenant"), this.appProps.getProperty("openstack.url"), this.appProps.getProperty("openstack.region"));
        }
        return this.openStackClient;
    }

    public AwsClient getAwsClient() {
        if (this.awsClient == null) {
            this.awsClient = new AwsClient();
        }
        return this.awsClient;
    }

    public void registerOrchestratorLocation(String orchestratorId, String locationId, String locationName) {
        if (orchestratorLocationIds == null) {
            orchestratorLocationIds = Maps.newHashMap();
        }
        Map<String, String> locations = orchestratorLocationIds.get(orchestratorId);
        if (locations == null) {
            locations = Maps.newHashMap();
            orchestratorLocationIds.put(orchestratorId, locations);
        }
        locations.put(locationName, locationId);
    }

    public String getLocationId(String orchestratorId, String locationName) {
        return orchestratorLocationIds.get(orchestratorId).get(locationName);
    }

    public void registerOrchestratorLocationResource(String orchestratorId, String locationId, String resourceId, String resourceName) {
        if (orchestratorLocationResourceIds == null) {
            orchestratorLocationResourceIds = Maps.newHashMap();
        }
        Map<String, Map<String, String>> locations = orchestratorLocationResourceIds.get(orchestratorId);
        if (locations == null) {
            locations = Maps.newHashMap();
            orchestratorLocationResourceIds.put(orchestratorId, locations);
        }
        Map<String, String> resources = locations.get(locationId);
        if (resources == null) {
            resources = Maps.newHashMap();
            locations.put(locationId, resources);
        }
        resources.put(resourceName, resourceId);
    }

    public String getLocationResourceId(String orchestratorId, String locationId, String resourceName) {
        return orchestratorLocationResourceIds.get(orchestratorId).get(locationId).get(resourceName);
    }

    public void setCurrentWorkflowName(String workflowName) {
        currentWorkflowName = workflowName;
    }

    public String getCurrentWorkflowName() {
        return currentWorkflowName;
    }

    public void setCsarGitRepositoryId(String id) {
        this.csarGitRepositoryId = id;
    }

    public String getCsarGitRepositoryId() {
        return this.csarGitRepositoryId;
    }

    public void registerStringContent(String key, String value) {
        this.stringContent.put(key, value);
    }

    public String getRegisteredStringContent(String key) {
        return this.stringContent.get(key);
    }

    public String getCurrentExternalId() {
        return currentExternalId;
    }

    public void setCurrentExternalId(String currentExternalId) {
        this.currentExternalId = currentExternalId;
    }
}
