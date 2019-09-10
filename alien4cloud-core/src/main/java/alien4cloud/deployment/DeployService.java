package alien4cloud.deployment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.secret.services.SecretProviderService;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.deployment.model.SecretProviderCredentials;
import alien4cloud.events.DeploymentCreatedEvent;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSourceType;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.orchestrators.Orchestrator;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.orchestrators.services.OrchestratorService;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.EmptyMetaPropertyException;
import alien4cloud.paas.exception.OrchestratorDeploymentIdConflictException;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentLog;
import alien4cloud.paas.model.PaaSDeploymentLogLevel;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.security.model.User;
import alien4cloud.utils.PropertyUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for deployment of an application.
 */
@Slf4j
@Service
public class DeployService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Inject
    private OrchestratorService orchestratorService;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Inject
    private ArtifactProcessorService artifactProcessorService;
    @Inject
    private ApplicationEventPublisher eventPublisher;
    @Inject
    private DeploymentLockService deploymentLockService;
    @Inject
    private DeploymentLoggingService deploymentLoggingService;
    @Inject
    private ServiceResourceRelationshipService serviceResourceRelationshipService;
    @Inject
    private SecretProviderService secretProviderService;

    /**
     * Deploy a topology and return the deployment ID.
     *
     * @param deploymentTopology Location aware and matched topology.
     * @param deploymentSource Application to be deployed or the Csar that contains test toplogy to be deployed
     * @return The id of the generated deployment.
     */
    public String deploy(final User deployer, final SecretProviderCredentials secretProviderCredentials, final DeploymentTopology deploymentTopology,
            IDeploymentSource deploymentSource) {
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        final Location firstLocation = locations.values().iterator().next();
        String deploymentPaaSId = generateOrchestratorDeploymentId(deploymentTopology.getEnvironmentId(), firstLocation.getOrchestratorId());
        return deploymentLockService.doWithDeploymentWriteLock(deploymentPaaSId, () -> {
            // Get the orchestrator that will perform the deployment
            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(firstLocation.getOrchestratorId());

            // Create a deployment object to be kept in ES.
            final Deployment deployment = new Deployment();
            deployment.setId(UUID.randomUUID().toString());
            deployment.setOrchestratorId(firstLocation.getOrchestratorId());
            deployment.setLocationIds(locationIds.values().toArray(new String[locationIds.size()]));
            deployment.setOrchestratorDeploymentId(deploymentPaaSId);
            deployment.setSourceId(deploymentSource.getId());
            deployment.setDeployerUsername(deployer.getUsername());
            String sourceName;
            if (deploymentSource.getName() == null) {
                sourceName = UUID.randomUUID().toString();
            } else {
                sourceName = deploymentSource.getName();
            }
            deployment.setSourceName(sourceName);
            deployment.setSourceType(DeploymentSourceType.fromSourceType(deploymentSource.getClass()));
            // mandatory for the moment since we could have deployment with no environment (csar test)
            deployment.setEnvironmentId(deploymentTopology.getEnvironmentId());
            deployment.setVersionId(deploymentTopology.getVersionId());
            deployment.setStartDate(new Date());
            setUsedServicesResourcesIds(deploymentTopology, deployment);
            alienDao.save(deployment);
            // publish an event for the eventual managed service
            eventPublisher.publishEvent(new DeploymentCreatedEvent(this, deployment.getId()));

            PaaSTopologyDeploymentContext deploymentContext = saveDeploymentTopologyAndGenerateDeploymentContext(secretProviderCredentials, deploymentTopology,
                    deployment, locations);

            // Build the context for deployment and deploy
            orchestratorPlugin.deploy(deploymentContext, new IPaaSCallback<Object>() {
                @Override
                public void onSuccess(Object data) {
                    log.debug("Deployed topology [{}] on location [{}], generated deployment with id [{}]", deploymentTopology.getInitialTopologyId(),
                            firstLocation.getId(), deployment.getId());
                }

                @Override
                public void onFailure(Throwable t) {
                    log.error("Deployment failed with cause", t);
                    log(deployment, t);

                }
            });
            log.debug("Triggered deployment of topology [{}] on location [{}], generated deployment with id [{}]", deploymentTopology.getInitialTopologyId(),
                    firstLocation.getId(), deployment.getId());
            return deployment.getId();
        });
    }

    public void update(SecretProviderCredentials secretProviderCredentials, final DeploymentTopology deploymentTopology,
            final IDeploymentSource deploymentSource, final Deployment existingDeployment, final IPaaSCallback<Object> callback) {
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        final Location firstLocation = locations.values().iterator().next();

        deploymentLockService.doWithDeploymentWriteLock(existingDeployment.getOrchestratorDeploymentId(), () -> {
            // Get the orchestrator that will perform the deployment
            IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(firstLocation.getOrchestratorId());

            PaaSTopologyDeploymentContext deploymentContext = saveDeploymentTopologyAndGenerateDeploymentContext(secretProviderCredentials, deploymentTopology,
                    existingDeployment, locations);

            // After update we allow running a post_update workflow automatically, however as adding workflow in update depends on orchestrator we have to check
            // if such option is possible on the selected orchestrator.
            final DeploymentTopology deployedTopology = alienMonitorDao.findById(DeploymentTopology.class, existingDeployment.getId());

            // enrich the callback
            IPaaSCallback<Object> callbackWrapper = new IPaaSCallback<Object>() {
                @Override
                public void onSuccess(Object data) {
                    existingDeployment.setVersionId(deploymentTopology.getVersionId());
                    alienDao.save(existingDeployment);
                    // Trigger post update workflow if defined in both the initial and current topologies.
                    if (deploymentTopology.getWorkflows().get(NormativeWorkflowNameConstants.POST_UPDATE) != null
                            && deployedTopology.getWorkflows().get(NormativeWorkflowNameConstants.POST_UPDATE) != null) {
                        scheduler.execute(() -> tryLaunchingPostUpdateWorkflow(System.currentTimeMillis(), existingDeployment, orchestratorPlugin,
                                deploymentContext, callback));
                    } else {
                        callback.onSuccess(data);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log(existingDeployment, throwable);
                    callback.onFailure(throwable);
                }
            };
            // Build the context for deployment and deploy
            orchestratorPlugin.update(deploymentContext, callbackWrapper);
            log.debug("Triggered deployment of topology [{}] on location [{}], generated deployment with id [{}]", deploymentTopology.getInitialTopologyId(),
                    firstLocation.getId(), existingDeployment.getId());

            return null;
        });
    }

    private long timeout = 60 * 60 * 1000;

    ListeningScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(4));
    }

    private void tryLaunchingPostUpdateWorkflow(long startTime, Deployment existingDeployment, IOrchestratorPlugin orchestratorPlugin,
            PaaSTopologyDeploymentContext deploymentContext, IPaaSCallback<Object> callback) {
        // We have to wait for status to be update success
        if ((System.currentTimeMillis() - startTime) < timeout) {
            orchestratorPlugin.getStatus(deploymentContext, new IPaaSCallback<DeploymentStatus>() {
                @Override
                public void onSuccess(DeploymentStatus data) {
                    if (data != null && DeploymentStatus.UPDATED.equals(data)) {
                        log(existingDeployment, "Launching post update workflow", null, PaaSDeploymentLogLevel.INFO);
                        orchestratorPlugin.launchWorkflow(deploymentContext, NormativeWorkflowNameConstants.POST_UPDATE, Maps.newHashMap(),
                                new IPaaSCallback<String>() {
                                    @Override
                                    public void onSuccess(String data) {
                                        callback.onSuccess(data);
                                        log(existingDeployment, "Post update workflow execution completed successfully", null, PaaSDeploymentLogLevel.INFO);
                                    }

                                    @Override
                                    public void onFailure(Throwable throwable) {
                                        callback.onFailure(throwable);
                                        log(existingDeployment, throwable);
                                    }
                                });
                    } else if (data != null && DeploymentStatus.UPDATE_FAILURE.equals(data)) {
                        log(existingDeployment, "Update failed, not launching post update workflow.", null, PaaSDeploymentLogLevel.WARN);
                    } else {
                        // re-schedule it in one second
                        scheduler.schedule(() -> tryLaunchingPostUpdateWorkflow(startTime, existingDeployment, orchestratorPlugin, deploymentContext, callback),
                                1, TimeUnit.SECONDS);
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log(existingDeployment,
                            "Error while waiting to launch post update workflow. Please do it yourself after update is completed. " + throwable.getMessage(),
                            ExceptionUtils.getStackTrace(throwable), PaaSDeploymentLogLevel.ERROR);
                }
            });
        } else {
            log(existingDeployment, "Timeout reached before launch of post update workflow. Please do it yourself after update is completed.", null,
                    PaaSDeploymentLogLevel.ERROR);
        }
    }

    private void log(Deployment deployment, Throwable throwable) {
        log(deployment, throwable.getMessage(), ExceptionUtils.getStackTrace(throwable), PaaSDeploymentLogLevel.ERROR);
    }

    private void log(Deployment deployment, String message, String stack, PaaSDeploymentLogLevel level) {
        PaaSDeploymentLog deploymentLog = new PaaSDeploymentLog();
        deploymentLog.setDeploymentId(deployment.getId());
        deploymentLog.setDeploymentPaaSId(deployment.getOrchestratorDeploymentId());
        String content = stack == null ? message : message + "\n" + stack;
        deploymentLog.setContent(content);
        deploymentLog.setLevel(level);
        deploymentLog.setTimestamp(new Date());
        deploymentLoggingService.save(deploymentLog);

        PaaSMessageMonitorEvent messageMonitorEvent = new PaaSMessageMonitorEvent();
        messageMonitorEvent.setDeploymentId(deploymentLog.getDeploymentId());
        messageMonitorEvent.setOrchestratorId(deploymentLog.getDeploymentPaaSId());
        messageMonitorEvent.setMessage(message);
        messageMonitorEvent.setDate(deploymentLog.getTimestamp().getTime());
        alienMonitorDao.save(messageMonitorEvent);

    }

    private PaaSTopologyDeploymentContext saveDeploymentTopologyAndGenerateDeploymentContext(final SecretProviderCredentials secretProviderCredentials,
            final DeploymentTopology deploymentTopology, final Deployment deployment, final Map<String, Location> locations) {
        String deploymentTopologyId = deploymentTopology.getId();
        // save the topology as a deployed topology.
        // change the Id before saving
        deploymentTopology.setId(deployment.getId());
        deploymentTopology.setDeployed(true);

        alienMonitorDao.save(deploymentTopology);
        // put back the old Id for deployment
        deploymentTopology.setId(deploymentTopologyId);
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = null;
        if (secretProviderCredentials != null) {
            secretProviderConfigurationAndCredentials = secretProviderService.generateToken(locations, secretProviderCredentials.getPluginName(),
                    secretProviderCredentials.getCredentials());
        }

        PaaSTopologyDeploymentContext deploymentContext = deploymentContextService.buildTopologyDeploymentContext(secretProviderConfigurationAndCredentials,
                deployment, locations, deploymentTopology);
        // Process services relationships to inject the service side based on the service resource.
        serviceResourceRelationshipService.process(deploymentContext);
        // Download and process all remote artifacts before deployment
        artifactProcessorService.processArtifacts(deploymentContext);

        return deploymentContext;
    }

    /**
     * From the substitutedNodes values, find out which are services and populate the {@link Deployment#serviceResourceIds}
     * 
     * @param deployment
     * @param deploymentTopology
     */
    private void setUsedServicesResourcesIds(DeploymentTopology deploymentTopology, Deployment deployment) {
        String[] serviceResourcesIds = deploymentTopology.getSubstitutedNodes().entrySet().stream()
                .filter(entry -> deploymentTopology.getNodeTemplates().get(entry.getKey()) instanceof ServiceNodeTemplate).map(entry -> entry.getValue())
                .toArray(String[]::new);

        if (ArrayUtils.isNotEmpty(serviceResourcesIds)) {
            deployment.setServiceResourceIds(serviceResourcesIds);
        }
    }

    /**
     * Generate the human readable deployment id for the orchestrator.
     *
     * @param envId Id of the deployed environment.
     * @param orchestratorId Id of the orchestrator on which the deployment is performed.
     * @return The orchestrator deployment id.
     * @throws alien4cloud.paas.exception.OrchestratorDeploymentIdConflictException
     */
    private String generateOrchestratorDeploymentId(String envId, String orchestratorId) throws OrchestratorDeploymentIdConflictException {
        log.debug("Generating deployment paaS Id...");
        log.debug("All spaces will be replaced by an \"_\" charactr. You might consider it while naming your applications.");
        ApplicationEnvironment env = applicationEnvironmentService.getOrFail(envId);
        Orchestrator orchestrator = orchestratorService.getOrFail(orchestratorId);
        String namePattern = orchestrator.getDeploymentNamePattern();
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(namePattern);
        String orchestratorDeploymentId = (String) exp
                .getValue(new OrchestratorIdContext(env, applicationService.getOrFail(env.getApplicationId()), namePattern.contains("metaProperties[")));
        // ensure that the id is not used by another deployment.
        if (deploymentService.isActiveDeployment(orchestratorId, orchestratorDeploymentId)) {
            throw new OrchestratorDeploymentIdConflictException("Conflict detected with the generated paasId <" + orchestratorDeploymentId + ">.");
        }
        return orchestratorDeploymentId;
    }

    // Inner class used to build context for generation of the orchestrator id.
    @Getter
    class OrchestratorIdContext {
        private ApplicationEnvironment environment;
        private Application application;
        private Map<String, String> metaProperties;
        private String time;

        private Map<String, String> constructMapOfMetaProperties(final Application app) {
            Map<String, String[]> filters = new HashMap<String, String[]>();
            filters.put("target", new String[] { "application" }); // get all meta properties configuration for applications.
            FacetedSearchResult result = alienDao.facetedSearch(MetaPropConfiguration.class, null, filters, null, 0, 20);
            MetaPropConfiguration metaProp;
            Map<String, String> metaProperties = Maps.newHashMap();
            for (int i = 0; i < result.getData().length; i++) {
                metaProp = (MetaPropConfiguration) result.getData()[i];
                if (app.getMetaProperties().get(metaProp.getId()) != null) {
                    metaProperties.put(metaProp.getName(), app.getMetaProperties().get(metaProp.getId()));
                } else if (!PropertyUtil.setScalarDefaultValueIfNotNull(metaProperties, metaProp.getName(), metaProp.getDefault())) {
                    throw new EmptyMetaPropertyException("The meta property " + metaProp.getName() + " is null and don't have a default value.");
                }
            }
            return metaProperties;
        }

        public OrchestratorIdContext(ApplicationEnvironment env, Application app, boolean hasMetaProperties) {
            this.environment = env;
            this.application = app;
            SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddHHmm");
            this.time = ft.format(new Date());
            if (hasMetaProperties) {
                this.metaProperties = constructMapOfMetaProperties(app);
            }
        }
    }
}