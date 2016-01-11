package alien4cloud.cloud;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.mockito.Mockito;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorConfigurationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.PluginConfigurationException;

import com.google.common.collect.Lists;

public class CloudServiceTest {
    public static final String DEFAULT_CLOUD_CONFIGURATION = "This is the cloud configuration";
    private OrchestratorPluginService orchestratorPluginService;
    private OrchestratorConfigurationService orchestratorConfigurationService;
    private IGenericSearchDAO alienDAO;
    private DeploymentService deploymentService;
    private OrchestratorStateService orchestratorStateService;
    private OrchestratorService orchestratorService;

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
        orchestratorConfigurationService = Mockito.mock(OrchestratorConfigurationService.class);
        orchestratorPluginService = Mockito.mock(OrchestratorPluginService.class);
        deploymentService = Mockito.mock(DeploymentService.class);
        orchestratorService = Mockito.mock(OrchestratorService.class);
        // initialize orchestrator state service instance with mocks
        orchestratorStateService = new OrchestratorStateService();
        setPrivateField(orchestratorStateService, "alienDAO", alienDAO);
        setPrivateField(orchestratorStateService, "orchestratorConfigurationService", orchestratorConfigurationService);
        setPrivateField(orchestratorStateService, "orchestratorPluginService", orchestratorPluginService);
        setPrivateField(orchestratorStateService, "deploymentService", deploymentService);
        setPrivateField(orchestratorStateService, "orchestratorService", orchestratorService);
    }

    @Test
    public void testInitializeNullData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        List<Orchestrator> enabledClouds = Lists.newArrayList();
        initSearch(enabledClouds);

        orchestratorStateService.initialize();
    }

    @Test
    public void testInitializeEmptyData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedCloudService();

        List<Orchestrator> enabledClouds = Lists.newArrayList(new Orchestrator[0]);
        initSearch(enabledClouds);

        orchestratorStateService.initialize();
    }

    @SuppressWarnings("unchecked")
    private void initSearch(List<Orchestrator> enabledClouds) {
        Mockito.when(orchestratorService.getAllEnabledOrchestrators()).thenReturn(enabledClouds);
    }

    private List<Orchestrator> searchCloud() {
        Orchestrator cloud = new Orchestrator();
        cloud.setId("id");
        cloud.setName("name");
        cloud.setState(OrchestratorState.CONNECTED);
        cloud.setPluginId("paasPluginId");
        cloud.setPluginBean("paasPluginBean");

        return Lists.newArrayList(cloud);
    }

    private void initializeAndWait() throws ExecutionException, InterruptedException {
        orchestratorStateService.initialize().get();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeConfigurableCloudValidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException,
            PluginConfigurationException, ExecutionException, InterruptedException, IOException {
        initializeMockedCloudService();

        IOrchestratorPluginFactory orchestratorPluginFactory = Mockito.mock(IOrchestratorPluginFactory.class);
        IOrchestratorPlugin orchestratorPlugin = Mockito.mock(IOrchestratorPlugin.class);

        List<Orchestrator> enabledClouds = searchCloud();
        Orchestrator cloud = enabledClouds.get(0);
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(cloud.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledClouds);

        Mockito.when(orchestratorService.getPluginFactory(cloud)).thenReturn(orchestratorPluginFactory);
        Mockito.when(orchestratorPluginFactory.newInstance()).thenReturn(orchestratorPlugin);
        Mockito.when(orchestratorPluginFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(orchestratorPluginFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.configurationAsValidObject(cloud.getId(), configuration.getConfiguration())).thenReturn(
                DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.getConfigurationOrFail(cloud.getId())).thenReturn(configuration);
        initializeAndWait();

        Mockito.verify(orchestratorPlugin, Mockito.times(1)).setConfiguration((String) configuration.getConfiguration());
        Mockito.verify(orchestratorPluginService, Mockito.times(1)).register(cloud.getId(), orchestratorPlugin);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeConfigurableCloudInvalidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, PluginConfigurationException, ExecutionException, InterruptedException, IOException {
        initializeMockedCloudService();

        IOrchestratorPluginFactory orchestratorPluginFactory = Mockito.mock(IOrchestratorPluginFactory.class);
        IOrchestratorPlugin orchestratorPlugin = Mockito.mock(IOrchestratorPlugin.class);

        List<Orchestrator> enabledClouds = searchCloud();
        Orchestrator cloud = enabledClouds.get(0);
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(cloud.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledClouds);

        Mockito.when(orchestratorService.getPluginFactory(cloud)).thenReturn(orchestratorPluginFactory);
        Mockito.when(orchestratorPluginFactory.newInstance()).thenReturn(orchestratorPlugin);
        Mockito.when(orchestratorPluginFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(orchestratorPluginFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.getConfigurationOrFail(cloud.getId())).thenReturn(configuration);
        Mockito.when(orchestratorConfigurationService.configurationAsValidObject(cloud.getId(), configuration.getConfiguration())).thenReturn(
                DEFAULT_CLOUD_CONFIGURATION);

        Mockito.doThrow(PluginConfigurationException.class).when(orchestratorPlugin).setConfiguration((String) configuration.getConfiguration());

        initializeAndWait();

        Mockito.verify(orchestratorPluginService, Mockito.times(0)).register(cloud.getId(), orchestratorPlugin);
        cloud = (Orchestrator) searchCloud().get(0);
        cloud.setState(OrchestratorState.DISABLED);
        Mockito.verify(alienDAO, Mockito.times(2)).save(Mockito.refEq(cloud));
    }
}