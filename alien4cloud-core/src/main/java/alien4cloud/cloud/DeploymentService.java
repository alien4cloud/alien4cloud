package alien4cloud.cloud;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.mapping.QueryHelper;
import org.elasticsearch.mapping.QueryHelper.SearchQueryHelperBuilder;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.Cloud;
import alien4cloud.model.common.MetaPropConfiguration;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentSourceType;
import alien4cloud.model.deployment.IDeploymentSource;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.exception.CloudDisabledException;
import alien4cloud.paas.exception.EmptyMetaPropertyException;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSTopology;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.utils.MapUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Manage deployment operations on a cloud.
 */
@Component
@Slf4j
public class DeploymentService {
    @Resource
    private QueryHelper queryHelper;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Resource
    private CloudService cloudService;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;
    @Resource
    private DeploymentSetupService deploymentSetupService;
    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    /**
     * Get deployments for a given cloud
     *
     * @param cloudId Id of the cloud for which to get deployments (can be null to get deployments for all clouds).
     * @param sourceId Id of the application for which to get deployments (can be null to get deployments for all applications).
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} that contains deployments.
     */
    public GetMultipleDataResult<Deployment> getDeployments(String cloudId, String sourceId, int from, int size) {
        return alienDao.search(Deployment.class, null, getDeploymentFilters(cloudId, sourceId, null), from, size);
    }

    /**
     * Get events for a specific deployment from an environment
     *
     * @param applicationEnvironmentId The environment we want to get events from
     * @param from The initial position of the events to get (based on time desc sorting)
     * @param size The number of events to get.
     * @return A result that contains all events.
     */
    public GetMultipleDataResult<?> getDeploymentEvents(String applicationEnvironmentId, int from, int size) {
        Deployment deployment = getActiveDeploymentFailIfNotExists(applicationEnvironmentId);
        return searchEvents(from, size, deployment);
    }

    private GetMultipleDataResult<?> searchEvents(int from, int size, Deployment deployment) {
        String index = alienMonitorDao.getIndexForType(AbstractMonitorEvent.class);
        SearchQueryHelperBuilder searchQueryHelperBuilder = queryHelper
                .buildSearchQuery(index)
                .types(PaaSDeploymentStatusMonitorEvent.class, PaaSInstanceStateMonitorEvent.class, PaaSMessageMonitorEvent.class,
                        PaaSInstanceStorageMonitorEvent.class)
                .filters(MapUtil.newHashMap(new String[] { "deploymentId" }, new String[][] { new String[] { deployment.getId() } }))
                .fieldSort("_timestamp", true);
        return alienMonitorDao.search(searchQueryHelperBuilder, from, size);
    }

    /**
     * Deploy a topology and return the deployment ID
     *
     * @param topology The topology to be deployed.
     * @param deploymentSource Application to be deployed or the Csar that contains test toplogy to be deployed
     * @param deploymentSetup DeploymentSetup used to deploy
     * @param cloudId If of the cloud on which to deploy.
     * @return The id of the generated deployment.
     * @throws CloudDisabledException In case the cloud is actually disabled and no deployments can be performed on this cloud.
     */
    public synchronized String deployTopology(Topology topology, IDeploymentSource deploymentSource, DeploymentSetup deploymentSetup, String cloudId)
            throws CloudDisabledException {
        log.info("Deploying topology [{}] on cloud [{}]", topology.getId(), cloudId);
        String topologyId = topology.getId();

        // Get underlying paaS provider of the cloud
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(cloudId);

        // create a deployment object for the given cloud.
        Deployment deployment = new Deployment();
        deployment.setCloudId(cloudId);
        deployment.setId(UUID.randomUUID().toString());
        deployment.setPaasId(generatePaasId(deploymentSetup.getEnvironmentId(), cloudId));
        deployment.setSourceId(deploymentSource.getId());
        String sourceName;
        if (deploymentSource.getName() == null) {
            sourceName = UUID.randomUUID().toString();
        } else {
            sourceName = deploymentSource.getName();
        }
        deployment.setSourceName(sourceName);
        deployment.setSourceType(DeploymentSourceType.fromSourceType(deploymentSource.getClass()));
        deployment.setStartDate(new Date());
        deployment.setDeploymentSetup(deploymentSetup);
        // mandatory for the moment since we could have deployment with no environment (csar test)
        deployment.setTopologyId(topologyId);

        alienDao.save(deployment);
        // save the topology as a deployed topology.
        // change the Id before saving
        topology.setId(deployment.getId());
        alienMonitorDao.save(topology);
        // put back the old Id for deployment
        topology.setId(topologyId);
        // Build the context for deployment and deploy
        paaSProvider.deploy(buildTopologyDeploymentContext(deployment, topology), null);
        log.info("Deployed topology [{}] on cloud [{}], generated deployment with id [{}]", topology.getId(), cloudId, deployment.getId());
        return deployment.getId();
    }

    private String generatePaasId(String envId, String cloudId) {
        // Inner class to get value from multiples objects
        @Getter
        class ContextObjectToParse {
            private ApplicationEnvironment environment;
            private Application application;
            private Map<String, String> metaProperties;
            private String time;

            private Map<String, String> constructMapOfMetaProperties(final Application app) {
                Map<String, String[]> filters = new HashMap<String, String[]>();
                filters.put("application", new String[] { "id" });
                FacetedSearchResult result = alienDao.facetedSearch(MetaPropConfiguration.class, null, filters, null, 0, 20);
                MetaPropConfiguration metaProp;
                Map<String, String> metaProperties = Maps.newHashMap();
                for (int i = 0; i < result.getData().length; i++) {
                    metaProp = (MetaPropConfiguration) result.getData()[i];
                    if (app.getMetaProperties().get(metaProp.getId()) != null) {
                        metaProperties.put(metaProp.getName(), app.getMetaProperties().get(metaProp.getId()));
                    } else if (metaProp.getDefault() != null) {
                        metaProperties.put(metaProp.getName(), metaProp.getDefault());
                    } else {
                        throw new EmptyMetaPropertyException("The meta property " + metaProp.getName() + " is null and don't have a default value.");
                    }
                }
                return metaProperties;
            }

            public ContextObjectToParse(ApplicationEnvironment env, Application app, boolean hasMetaProperties) {
                this.environment = env;
                this.application = app;
                SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddHHmm");
                this.time = ft.format(new Date());
                if (hasMetaProperties) {
                    this.metaProperties = constructMapOfMetaProperties(app);
                }
            }
        }

        ApplicationEnvironment env = applicationEnvironmentService.getOrFail(envId);
        Cloud cloud = cloudService.get(cloudId);
        String namePattern = cloud.getDeploymentNamePattern();
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(namePattern);
        return (String) exp.getValue(new ContextObjectToParse(env, applicationService.getOrFail(env.getApplicationId()), namePattern
                .contains("metaProperties[")));
    }

    private PaaSDeploymentContext buildDeploymentContext(Deployment deployment) {
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext();
        deploymentContext.setDeployment(deployment);
        return deploymentContext;
    }

    private PaaSTopologyDeploymentContext buildTopologyDeploymentContext(Deployment deployment, Topology topology) {
        PaaSTopologyDeploymentContext topologyDeploymentContext = new PaaSTopologyDeploymentContext();
        topologyDeploymentContext.setDeployment(deployment);
        Map<String, PaaSNodeTemplate> paaSNodes = topologyTreeBuilderService.buildPaaSNodeTemplate(topology);
        PaaSTopology paaSTopology = topologyTreeBuilderService.buildPaaSTopology(paaSNodes);
        topologyDeploymentContext.setPaaSTopology(paaSTopology);
        topologyDeploymentContext.setTopology(topology);
        topologyDeploymentContext.setDeployment(deployment);
        return topologyDeploymentContext;
    }

    /**
     * Un-deploy a topology.
     *
     * @param deploymentSetup setup object containing information to deploy
     * @throws CloudDisabledException In case the cloud selected for the topology is disabled.
     */
    public synchronized void undeployTopology(DeploymentSetup deploymentSetup) throws CloudDisabledException {
        String topologyId = deploymentSetupService.getTopologyId(deploymentSetup.getId());
        ApplicationEnvironment environment = deploymentSetupService.getApplicationEnvironment(deploymentSetup.getId());
        log.info("Un-deploying topology [{}] on cloud [{}]", topologyId, environment.getCloudId());
        Deployment activeDeployment = getActiveDeploymentFailIfNotExists(environment.getId());
        this.undeploy(activeDeployment);
    }

    /**
     * Un-deploy a deployment object
     *
     * @param deploymentId deployment id to deploy
     * @throws CloudDisabledException
     */
    public synchronized void undeploy(String deploymentId) throws CloudDisabledException {
        Deployment deployment = getMandatoryDeployment(deploymentId);
        undeploy(deployment);
    }

    private void undeploy(Deployment deployment) throws CloudDisabledException {
        String cloudId = deployment.getCloudId();
        log.info("Un-deploying deployment [{}] on cloud [{}]", deployment.getId(), cloudId);
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(cloudId);
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        paaSProvider.undeploy(deploymentContext, null);
        alienDao.save(deployment);
        log.info("Un-deployed deployment [{}] on cloud [{}]", deployment.getId(), cloudId);
    }

    private Deployment getMandatoryDeployment(String deploymentId) {
        Deployment deployment = alienDao.findById(Deployment.class, deploymentId);
        if (deployment == null) {
            throw new NotFoundException("Deployment <" + deploymentId + "> doesn't exists.");
        }
        return deployment;
    }

    /**
     * Scale up/down a node in a topology
     *
     * @param applicationEnvironmentId id of the targeted environment
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     * @throws CloudDisabledException In case the cloud selected for the topology is disabled.
     */
    public void scale(String applicationEnvironmentId, String nodeTemplateId, int instances) throws CloudDisabledException {
        Deployment deployment = getActiveDeploymentFailIfNotExists(applicationEnvironmentId);
        Topology topology = alienMonitorDao.findById(Topology.class, deployment.getId());
        // change the initial instance to the current instances for the runtime topology.
        int initialInstances = topology.getScalingPolicies().get(nodeTemplateId).getInitialInstances();
        topology.getScalingPolicies().get(nodeTemplateId).setInitialInstances(initialInstances + instances);
        alienMonitorDao.save(topology);
        log.info("Scaling <{}> node from <{}> to <{}> (topology runtime updated)", nodeTemplateId, initialInstances, instances);
        // call the paas provider to scale the topology
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(deployment.getCloudId());
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        paaSProvider.scale(deploymentContext, nodeTemplateId, instances, null);
    }

    public void switchInstanceMaintenanceMode(String applicationEnvironmentId, String nodeTemplateId, String instanceId, boolean maintenanceModeOn)
            throws CloudDisabledException, MaintenanceModeException {
        Deployment deployment = getActiveDeploymentFailIfNotExists(applicationEnvironmentId);
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(deployment.getCloudId());
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        paaSProvider.switchInstanceMaintenanceMode(deploymentContext, nodeTemplateId, instanceId, maintenanceModeOn);
    }

    public void switchMaintenanceMode(String applicationEnvironmentId, boolean maintenanceModeOn) throws CloudDisabledException, MaintenanceModeException {
        Deployment deployment = getActiveDeploymentFailIfNotExists(applicationEnvironmentId);
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(deployment.getCloudId());
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        paaSProvider.switchMaintenanceMode(deploymentContext, maintenanceModeOn);
    }

    /**
     * Get the current deployment status for a topology.
     *
     * @param deployment deployment for which we want the status
     * @param callback that will be called when status is available*
     * @return The status of the topology.
     * @throws CloudDisabledException In case the cloud selected for the topology is disabled.
     */
    public void getDeploymentStatus(final Deployment deployment, final IPaaSCallback<DeploymentStatus> callback) throws CloudDisabledException {
        if (deployment == null) {
            callback.onSuccess(DeploymentStatus.UNDEPLOYED);
            return;
        }
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(deployment.getCloudId());
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        IPaaSCallback<DeploymentStatus> esCallback = new IPaaSCallback<DeploymentStatus>() {
            @Override
            public void onSuccess(DeploymentStatus data) {
                if (data == DeploymentStatus.UNDEPLOYED) {
                    deployment.setEndDate(new Date());
                    alienDao.save(deployment);
                }
                callback.onSuccess(data);
            }

            @Override
            public void onFailure(Throwable throwable) {
                callback.onFailure(throwable);
            }
        };
        paaSProvider.getStatus(deploymentContext, esCallback);
    }

    /**
     * Get the detailed status for each instance of each node template.
     *
     * @param deployment The deployment for witch to get the instance informations.
     * @param callback callback on witch to send the map of node template's id to map of instance's id to instance information.
     * @throws CloudDisabledException In case the cloud selected for the topology is disabled.
     */
    public void getInstancesInformation(final Deployment deployment, IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback)
            throws CloudDisabledException {
        Map<String, Map<String, InstanceInformation>> instancesInformation = Maps.newHashMap();
        if (deployment == null) {
            callback.onSuccess(instancesInformation);
            return;
        }
        Topology runtimeTopology = alienMonitorDao.findById(Topology.class, deployment.getId());
        PaaSDeploymentContext deploymentContext = buildDeploymentContext(deployment);
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(deployment.getCloudId());
        paaSProvider.getInstancesInformation(deploymentContext, runtimeTopology, callback);
    }

    /**
     * Trigger the execution of an operation on a node.
     *
     * @param request the operation's execution description ( see {@link OperationExecRequest})
     * @throws CloudDisabledException In case the cloud selected for the topology is disabled.
     * @throws OperationExecutionException runtime exception during an operation
     */
    public void triggerOperationExecution(OperationExecRequest request, Topology topology, IPaaSCallback<Map<String, String>> callback)
            throws CloudDisabledException, OperationExecutionException {
        Deployment activeDeployment = getActiveDeploymentFailIfNotExists(request.getApplicationEnvironmentId());
        IPaaSProvider paaSProvider = cloudService.getPaaSProvider(activeDeployment.getCloudId());
        // It's a little bit ugly to let deployment setup to null but we do not need this information
        paaSProvider.executeOperation(buildTopologyDeploymentContext(activeDeployment, topology), request, callback);
    }

    /**
     * Get the deployed (runtime) topology of an application on a cloud
     *
     * @param applicationEnvironmentId id of the environment
     * @return the Topology requested if found
     */
    public Topology getRuntimeTopology(String applicationEnvironmentId) {
        Deployment deployment = getActiveDeploymentFailIfNotExists(applicationEnvironmentId);
        return alienMonitorDao.findById(Topology.class, deployment.getId());
    }

    /**
     * Get the deployed (runtime) topology of an application on a cloud
     *
     * @param topologyId id of the topology for which to get deployed topology.
     * @param cloudId targeted cloud id
     * @return the Topology requested if found
     */
    public Topology getRuntimeTopology(String topologyId, String cloudId) {
        Deployment deployment = getActiveDeploymentFailIfNotExists(topologyId, cloudId);
        return alienMonitorDao.findById(Topology.class, deployment.getId());
    }

    /**
     * Get an active Deployment given an environment
     * (if not exists throw exception)
     *
     * @param applicationEnvironmentId id of the environment
     * @return active deployment
     * @throws alien4cloud.exception.NotFoundException if not any deployment exists
     */
    public Deployment getActiveDeploymentFailIfNotExists(String applicationEnvironmentId) {
        Deployment deployment = getActiveDeployment(applicationEnvironmentId);
        if (deployment == null) {
            throw new NotFoundException("Deployment for environment <" + applicationEnvironmentId + "> doesn't exist.");
        }
        return deployment;
    }

    /**
     * Get an active deployment for a given cloud and topology
     * (if not exists throw exception)
     *
     * @param topologyId id of the topology that has been deployed
     * @param cloudId id of the target cloud
     * @return a deployment
     * @throws alien4cloud.exception.NotFoundException if not any deployment exists
     */
    public Deployment getActiveDeploymentFailIfNotExists(String topologyId, String cloudId) {
        Deployment deployment = getActiveDeployment(cloudId, topologyId);
        if (deployment == null) {
            throw new NotFoundException("Deployment for cloud <" + cloudId + "> and topology <" + topologyId + "> doesn't exist.");
        }
        return deployment;
    }

    /**
     * Get an active deployment for a given environment
     *
     * @param applicationEnvironmentId id of the environment
     * @return active deployment or null if not exist
     */
    public Deployment getActiveDeployment(String applicationEnvironmentId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "deploymentSetup.environmentId", "endDate" }, new String[][] {
                new String[] { applicationEnvironmentId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return dataResult.getData()[0];
        }
        return null;
    }

    /**
     * Get a topology for a given cloud / topology
     *
     * @param cloudId targeted cloud id
     * @param topologyId id of the topology to deploy
     * @return a deployment
     */
    public Deployment getActiveDeployment(String cloudId, String topologyId) {
        Deployment deployment = null;
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "cloudId", "topologyId", "endDate" }, new String[][] {
                new String[] { cloudId }, new String[] { topologyId }, new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            deployment = dataResult.getData()[0];
        }
        return deployment;
    }

    public Deployment[] getCloudActiveDeployments(String cloudId) {
        Map<String, String[]> activeDeploymentFilters = MapUtil.newHashMap(new String[] { "cloudId", "endDate" }, new String[][] { new String[] { cloudId },
                new String[] { null } });
        GetMultipleDataResult<Deployment> dataResult = alienDao.search(Deployment.class, null, activeDeploymentFilters, 1);
        return dataResult.getData();
    }

    public Map<String, PaaSTopologyDeploymentContext> getCloudActiveDeploymentContexts(String cloudId) {
        Deployment[] deployments = getCloudActiveDeployments(cloudId);
        Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts = Maps.newHashMap();
        for (Deployment deployment : deployments) {
            Topology topology = alienMonitorDao.findById(Topology.class, deployment.getId());
            activeDeploymentContexts.put(deployment.getPaasId(), buildTopologyDeploymentContext(deployment, topology));
        }
        return activeDeploymentContexts;
    }

    /**
     * Get a deployment given its id
     *
     * @param id id of the deployment
     * @return deployment with given id
     */
    public Deployment getDeployment(String id) {
        return alienDao.findById(Deployment.class, id);
    }

    /**
     * Get the filters to perform a search query on {@link Deployment} based on the cloudId and sourceId.
     *
     * @param cloudId Id of the cloud on which the application is deployed.
     * @param sourceId Id of the application that is deployed.
     * @param topologyId Id of the topology that is deployed.
     * @return The filters to get deployments.
     */
    private Map<String, String[]> getDeploymentFilters(String cloudId, String sourceId, String topologyId) {
        List<String> filterKeys = Lists.newArrayList();
        List<String[]> filterValues = Lists.newArrayList();
        if (cloudId != null) {
            filterKeys.add("cloudId");
            filterValues.add(new String[] { cloudId });
        }
        if (sourceId != null) {
            filterKeys.add("sourceId");
            filterValues.add(new String[] { sourceId });
        }
        if (topologyId != null) {
            filterKeys.add("topologyId");
            filterValues.add(new String[] { topologyId });
        }
        return MapUtil.newHashMap(filterKeys.toArray(new String[filterKeys.size()]), filterValues.toArray(new String[filterValues.size()][]));
    }

    /**
     * Get all deployments for a given deployment setup id
     *
     * @param deploymentSetupId deployment setup's id
     * @return deployment which have the given deployment setup id
     */
    public GetMultipleDataResult<Deployment> getDeploymentsByDeploymentSetup(String deploymentSetupId) {
        return alienDao.find(Deployment.class,
                MapUtil.newHashMap(new String[] { "deploymentSetup.id" }, new String[][] { new String[] { deploymentSetupId } }), Integer.MAX_VALUE);
    }

    /**
     * Find the unique active deployment from it's deployment name.
     *
     * @param deploymentPaaSId The deployment name (must be unique for an active deployment.
     * @return deployment which have the given deployment setup id
     */
    public Deployment getDeploymentByPaaSId(String deploymentPaaSId) {
        GetMultipleDataResult<Deployment> dataResult = alienDao.find(Deployment.class,
                MapUtil.newHashMap(new String[] { "paasId", "endDate" }, new String[][] { new String[] { deploymentPaaSId }, new String[] { null } }),
                Integer.MAX_VALUE);
        if (dataResult.getData() != null && dataResult.getData().length > 0) {
            return dataResult.getData()[0];
        }
        return null;
    }
}
