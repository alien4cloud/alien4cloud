package alien4cloud.orchestrator;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.OrchestratorConfiguration;
import alien4cloud.model.orchestrators.OrchestratorState;
import alien4cloud.orchestrators.locations.services.PluginArchiveIndexer;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.orchestrators.services.OrchestratorConfigurationService;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.orchestrators.services.OrchestratorStateService;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.PluginConfigurationException;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.Mockito;

public class OrchestratorServiceTest {
    public static final String DEFAULT_CLOUD_CONFIGURATION = "This is the cloud configuration";
    private OrchestratorPluginService orchestratorPluginService;
    private OrchestratorConfigurationService orchestratorConfigurationService;
    private IGenericSearchDAO alienDAO;
    private DeploymentService deploymentService;
    private OrchestratorStateService orchestratorStateService;
    private OrchestratorService orchestratorService;
    private PluginArchiveIndexer archiveIndexer;

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

    private void initializeMockedOrchestratorService() {
        alienDAO = Mockito.mock(IGenericSearchDAO.class);
        orchestratorConfigurationService = Mockito.mock(OrchestratorConfigurationService.class);
        orchestratorPluginService = Mockito.mock(OrchestratorPluginService.class);
        deploymentService = Mockito.mock(DeploymentService.class);
        orchestratorService = Mockito.mock(OrchestratorService.class);
        archiveIndexer = Mockito.mock(PluginArchiveIndexer.class);
        // initialize orchestrator state service instance with mocks
        orchestratorStateService = new OrchestratorStateService();
        setPrivateField(orchestratorStateService, "alienDAO", alienDAO);
        setPrivateField(orchestratorStateService, "orchestratorConfigurationService", orchestratorConfigurationService);
        setPrivateField(orchestratorStateService, "orchestratorPluginService", orchestratorPluginService);
        setPrivateField(orchestratorStateService, "deploymentService", deploymentService);
        setPrivateField(orchestratorStateService, "orchestratorService", orchestratorService);
        setPrivateField(orchestratorStateService, "archiveIndexer", archiveIndexer);
    }

    @Test
    public void testInitializeNullData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedOrchestratorService();

        List<Orchestrator> enabledOrchestrators = Lists.newArrayList();
        initSearch(enabledOrchestrators);

        orchestratorStateService.initialize();
    }

    @Test
    public void testInitializeEmptyData() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        initializeMockedOrchestratorService();

        List<Orchestrator> enabledOrchestrators = Lists.newArrayList(new Orchestrator[0]);
        initSearch(enabledOrchestrators);

        orchestratorStateService.initialize();
    }

    @SuppressWarnings("unchecked")
    private void initSearch(List<Orchestrator> enabledOrchestrators) {
        Mockito.when(orchestratorService.getAllEnabledOrchestrators()).thenReturn(enabledOrchestrators);
    }

    private List<Orchestrator> searchOrchestrator() {
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
    public void testInitializeConfigurableOrchestratorValidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, PluginConfigurationException, ExecutionException, InterruptedException, IOException {
        initializeMockedOrchestratorService();

        IOrchestratorPluginFactory orchestratorPluginFactory = Mockito.mock(IOrchestratorPluginFactory.class);
        IOrchestratorPlugin orchestratorPlugin = Mockito.mock(IOrchestratorPlugin.class);

        List<Orchestrator> enabledOrchestrators = searchOrchestrator();
        Orchestrator orchestrator = enabledOrchestrators.get(0);
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(orchestrator.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledOrchestrators);

        Mockito.when(orchestratorService.getPluginFactory(orchestrator)).thenReturn(orchestratorPluginFactory);
        Mockito.when(orchestratorPluginFactory.newInstance(DEFAULT_CLOUD_CONFIGURATION)).thenReturn(orchestratorPlugin);
        Mockito.when(orchestratorPluginFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(orchestratorPluginFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.configurationAsValidObject(orchestrator.getId(), configuration.getConfiguration()))
                .thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.getConfigurationOrFail(orchestrator.getId())).thenReturn(configuration);
        initializeAndWait();

        Mockito.verify(orchestratorPlugin, Mockito.times(1)).setConfiguration(orchestrator.getId(), configuration.getConfiguration());
        Mockito.verify(orchestratorPluginService, Mockito.times(1)).register(orchestrator.getId(), orchestratorPlugin);
        IOrchestratorPluginFactory fatory = orchestratorService.getPluginFactory(orchestrator);
        Mockito.verify(archiveIndexer, Mockito.times(1)).indexOrchestratorArchives(fatory, fatory.newInstance(DEFAULT_CLOUD_CONFIGURATION));
        ;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testInitializeConfigurableOrchstratorInvalidConfig() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            SecurityException, PluginConfigurationException, ExecutionException, InterruptedException, IOException {
        initializeMockedOrchestratorService();

        IOrchestratorPluginFactory orchestratorPluginFactory = Mockito.mock(IOrchestratorPluginFactory.class);
        IOrchestratorPlugin orchestratorPlugin = Mockito.mock(IOrchestratorPlugin.class);

        List<Orchestrator> enabledOrchestrators = searchOrchestrator();
        Orchestrator orchestrator = enabledOrchestrators.get(0);
        OrchestratorConfiguration configuration = new OrchestratorConfiguration(orchestrator.getId(), DEFAULT_CLOUD_CONFIGURATION);

        initSearch(enabledOrchestrators);

        Mockito.when(orchestratorService.getPluginFactory(orchestrator)).thenReturn(orchestratorPluginFactory);
        Mockito.when(orchestratorPluginFactory.newInstance(DEFAULT_CLOUD_CONFIGURATION)).thenReturn(orchestratorPlugin);
        Mockito.when(orchestratorPluginFactory.getConfigurationType()).thenReturn(String.class);
        Mockito.when(orchestratorPluginFactory.getDefaultConfiguration()).thenReturn(DEFAULT_CLOUD_CONFIGURATION);
        Mockito.when(orchestratorConfigurationService.getConfigurationOrFail(orchestrator.getId())).thenReturn(configuration);
        Mockito.when(orchestratorConfigurationService.configurationAsValidObject(orchestrator.getId(), configuration.getConfiguration()))
                .thenReturn(DEFAULT_CLOUD_CONFIGURATION);

        Mockito.doThrow(PluginConfigurationException.class).when(orchestratorPlugin).setConfiguration(orchestrator.getId(), configuration.getConfiguration());

        initializeAndWait();

        Mockito.verify(orchestratorPluginService, Mockito.times(0)).register(orchestrator.getId(), orchestratorPlugin);
        orchestrator = (Orchestrator) searchOrchestrator().get(0);
        orchestrator.setState(OrchestratorState.DISABLED);
        Mockito.verify(alienDAO, Mockito.times(2)).save(Mockito.refEq(orchestrator));
    }
}