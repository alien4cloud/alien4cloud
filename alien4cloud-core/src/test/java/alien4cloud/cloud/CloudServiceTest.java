package alien4cloud.cloud;

import java.lang.reflect.Field;

import org.elasticsearch.index.query.FilterBuilder;
import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.cloud.CloudConfiguration;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IConfigurablePaaSProviderFactory;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.IPaaSProviderFactory;
import alien4cloud.paas.PaaSProviderFactoriesService;
import alien4cloud.paas.PaaSProviderService;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.plan.MockPaaSProvider;

public class CloudServiceTest {

    public static final String DEFAULT_CLOUD_CONFIGURATION = "This is the cloud configuration";
    private PaaSProviderService paaSProviderService;
    private PaaSProviderFactoriesService paaSProviderFactoriesService;
    private IGenericSearchDAO alienDAO;
    private CloudImageService cloudImageService;
    private DeploymentService deploymentService;
    private CloudService cloudService;

    private void setPrivateField(Object target, String fieldName, Object fieldValue) {
        Field field;
        try {
            field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, fieldValue);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException("Test failed as we cannot set private field.", e);
        }
    }

    private void initializeMockedCloudService() {
        alienDAO = Mockito.mock(IGenericSearchDAO.class);
        paaSProviderFactoriesService = Mockito.mock(PaaSProviderFactoriesService.class);
        paaSProviderService = Mockito.mock(PaaSProviderService.class);
        cloudImageService = Mockito.mock(CloudImageService.class);
        deploymentService = Mockito.mock(DeploymentService.class);
        // initialize cloud service instance with mocks
        cloudService = new CloudService();
        setPrivateField(cloudService, "alienDAO", alienDAO);
        setPrivateField(cloudService, "paaSProviderFactoriesService", paaSProviderFactoriesService);
        setPrivateField(cloudService, "paaSProviderService", paaSProviderService);
        setPrivateField(cloudService, "cloudImageService", cloudImageService);
        setPrivateField(cloudService, "deploymentService", deploymentService);
    }

    @Test
    public void testInitializeNullData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        GetMultipleDataResult<Cloud> enabledClouds = new GetMultipleDataResult<Cloud>();
        initSearch(enabledClouds);

        cloudService.initialize();
    }

    @Test
    public void testInitializeEmptyData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        GetMultipleDataResult<Cloud> enabledClouds = new GetMultipleDataResult<Cloud>();
        enabledClouds.setData(new Cloud[0]);
        enabledClouds.setTypes(new String[0]);
        initSearch(enabledClouds);

        cloudService.initialize();
    }

    @SuppressWarnings("unchecked")
    private void initSearch(GetMultipleDataResult<Cloud> enabledClouds) {
        Mockito.when(
                alienDAO.search(Mockito.eq(Cloud.class), Mockito.isNull(String.class), Mockito.anyMap(), Mockito.isNull(FilterBuilder.class),
                        Mockito.isNull(String.class), Mockito.eq(0), Mockito.eq(20))).thenReturn(enabledClouds);
    }

    private GetMultipleDataResult<Cloud> searchCloud(boolean configurable) {
        Cloud cloud = new Cloud();
        cloud.setId("id");
        cloud.setName("name");
        cloud.setEnabled(true);
        cloud.setPaasPluginId("paasPluginId");
        cloud.setPaasPluginBean("paasPluginBean");
        cloud.setConfigurable(configurable);

        GetMultipleDataResult<Cloud> enabledClouds = new GetMultipleDataResult<Cloud>();
        enabledClouds.setData(new Cloud[] { cloud });
        enabledClouds.setTypes(new String[] { Cloud.class.getName() });
        return enabledClouds;
    }

    @Test
    public void testInitializeUnconfigurableCloud() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        IPaaSProviderFactory paaSProviderFactory = Mockito.mock(IPaaSProviderFactory.class);
        IPaaSProvider paaSProvider = Mockito.mock(IPaaSProvider.class);

        GetMultipleDataResult<Cloud> enabledClouds = searchCloud(false);
        Cloud cloud = enabledClouds.getData()[0];

        initSearch(enabledClouds);
        Mockito.when(paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean())).thenReturn(paaSProviderFactory);
        Mockito.when(paaSProviderFactory.newInstance()).thenReturn(paaSProvider);

        cloudService.initializeAndWait();

        Mockito.verify(paaSProviderService, Mockito.times(1)).register(cloud.getId(), paaSProvider);
    }

    @Test
    public void testInitializeConfigurableCloudNoConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        IPaaSProviderFactory paaSProviderFactory = Mockito.mock(IPaaSProviderFactory.class);
        IPaaSProvider paaSProvider = Mockito.mock(IPaaSProvider.class, Mockito.withSettings().extraInterfaces(IConfigurablePaaSProvider.class));

        GetMultipleDataResult<Cloud> enabledClouds = searchCloud(true);
        Cloud cloud = enabledClouds.getData()[0];

        initSearch(enabledClouds);
        Mockito.when(paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean())).thenReturn(paaSProviderFactory);
        Mockito.when(paaSProviderFactory.newInstance()).thenReturn(paaSProvider);
        Mockito.when(alienDAO.findById(CloudConfiguration.class, cloud.getId())).thenReturn(null);

        cloudService.initializeAndWait();

        Mockito.verify(paaSProviderService, Mockito.times(1)).register(cloud.getId(), paaSProvider);
    }

    @Test
    public void testInitializeConfigurableCloudInvalidPlugin() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        IConfigurablePaaSProviderFactory<String> paaSProviderFactory = Mockito.mock(IConfigurablePaaSProviderFactory.class);
        IConfigurablePaaSProvider<String> paaSProvider = Mockito.mock(IConfigurablePaaSProvider.class);

        GetMultipleDataResult<Cloud> enabledClouds = searchCloud(true);
        Cloud cloud = enabledClouds.getData()[0];
        CloudConfiguration configuration = new CloudConfiguration(cloud.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledClouds);
        Mockito.when(paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean())).thenReturn(paaSProviderFactory);
        Mockito.when(paaSProviderFactory.newInstance()).thenReturn(paaSProvider);
        Mockito.when(alienDAO.findById(CloudConfiguration.class, cloud.getId())).thenReturn(configuration);

        cloudService.initializeAndWait();

        Mockito.verify(paaSProviderService, Mockito.times(0)).register(cloud.getId(), paaSProvider);
        Mockito.verify(paaSProviderService, Mockito.times(1)).unregister(cloud.getId());
        cloud = (Cloud) searchCloud(true).getData()[0];
        cloud.setEnabled(false);
        Mockito.verify(alienDAO, Mockito.times(1)).save(Mockito.refEq(cloud));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeConfigurableCloudValidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException,
            PluginConfigurationException {
        initializeMockedCloudService();

        IConfigurablePaaSProviderFactory paaSProviderFactory = Mockito.mock(IConfigurablePaaSProviderFactory.class);
        IConfigurablePaaSProvider<String> paaSProvider = Mockito.mock(MockPaaSProvider.class);

        GetMultipleDataResult<Cloud> enabledClouds = searchCloud(true);
        Cloud cloud = enabledClouds.getData()[0];
        CloudConfiguration configuration = new CloudConfiguration(cloud.getId(), DEFAULT_CLOUD_CONFIGURATION);
        enabledClouds.setFrom(0);
        enabledClouds.setTotalResults(1);

        initSearch(enabledClouds);

        Mockito.when(paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean())).thenReturn(paaSProviderFactory);
        Mockito.when(paaSProviderFactory.newInstance()).thenReturn(paaSProvider);
        Mockito.when(paaSProviderFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(paaSProviderFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(alienDAO.findById(CloudConfiguration.class, cloud.getId())).thenReturn(configuration);
        cloudService.initializeAndWait();

        Mockito.verify(paaSProvider, Mockito.times(1)).setConfiguration((String) configuration.getConfiguration());
        Mockito.verify(paaSProviderService, Mockito.times(1)).register(cloud.getId(), paaSProvider);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeConfigurableCloudInvalidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, PluginConfigurationException {
        initializeMockedCloudService();

        IConfigurablePaaSProviderFactory<String> paaSProviderFactory = Mockito.mock(IConfigurablePaaSProviderFactory.class);
        IConfigurablePaaSProvider<String> paaSProvider = Mockito.mock(MockPaaSProvider.class);

        GetMultipleDataResult<Cloud> enabledClouds = searchCloud(true);
        Cloud cloud = enabledClouds.getData()[0];
        CloudConfiguration configuration = new CloudConfiguration(cloud.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledClouds);

        Mockito.when(paaSProviderFactoriesService.getPluginBean(cloud.getPaasPluginId(), cloud.getPaasPluginBean())).thenReturn(paaSProviderFactory);
        Mockito.when(paaSProviderFactory.newInstance()).thenReturn(paaSProvider);
        Mockito.when(paaSProviderFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(paaSProviderFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(alienDAO.findById(CloudConfiguration.class, cloud.getId())).thenReturn(configuration);
        Mockito.when(alienDAO.findById(CloudConfiguration.class, cloud.getId())).thenReturn(configuration);

        Mockito.doThrow(PluginConfigurationException.class).when(paaSProvider).setConfiguration((String) configuration.getConfiguration());

        cloudService.initializeAndWait();

        Mockito.verify(paaSProviderService, Mockito.times(0)).register(cloud.getId(), paaSProvider);
        Mockito.verify(paaSProviderService, Mockito.times(1)).unregister(cloud.getId());
        cloud = (Cloud) searchCloud(true).getData()[0];
        cloud.setEnabled(false);
        Mockito.verify(alienDAO, Mockito.times(1)).save(Mockito.refEq(cloud));
    }
}
