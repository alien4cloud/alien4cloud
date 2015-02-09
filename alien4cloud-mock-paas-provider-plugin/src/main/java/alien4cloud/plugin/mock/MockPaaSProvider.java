package alien4cloud.plugin.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import alien4cloud.cloud.DeploymentService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.Application;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.cloud.CloudResourceType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.IConfigurablePaaSProvider;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.InstanceStatus;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStorageMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.tosca.normative.ToscaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;

@Slf4j
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MockPaaSProvider extends AbstractPaaSProvider implements IConfigurablePaaSProvider<ProviderConfig> {

    public static final String PUBLIC_IP = "ip_address";
    public static final String TOSCA_ID = "tosca_id";
    public static final String TOSCA_NAME = "tosca_name";

    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private final Map<String, PropertyDefinition> deploymentProperties;
    private final Map<String, DeploymentStatus> deploymentsMap = Maps.newConcurrentMap();

    @Resource
    private DeploymentService deploymentService;

    private ProviderConfig providerConfiguration;

    /**
     * A little bit scary isn't it ? It's just a mock man.
     */
    private final Map<String, Map<String, Map<String, InstanceInformation>>> instanceInformationsMap = Maps.newConcurrentMap();

    private final List<AbstractMonitorEvent> toBeDeliveredEvents = Collections.synchronizedList(new ArrayList<AbstractMonitorEvent>());

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    private static final String UNKNOWN_APPLICATION_THAT_NEVER_WORKS = "UNKNOWN-APPLICATION";

    private static final String BAD_APPLICATION_THAT_NEVER_WORKS = "BAD-APPLICATION";

    private static final String WARN_APPLICATION_THAT_NEVER_WORKS = "WARN-APPLICATION";

    private static final String BLOCKSTORAGE_APPLICATION = "BLOCKSTORAGE-APPLICATION";

    public MockPaaSProvider() {
        deploymentProperties = Maps.newHashMap();
        executorService.scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                for (Map.Entry<String, Map<String, Map<String, InstanceInformation>>> topologyInfo : instanceInformationsMap.entrySet()) {
                    // Call this just to change instance state and push to client
                    doChangeInstanceInformations(topologyInfo.getKey(), topologyInfo.getValue());
                }
            }
        }, 1L, 1L, TimeUnit.SECONDS);

    }

    @Override
    public void updateMatcherConfig(CloudResourceMatcherConfig config) {
        // Do nothing
    }

    @PreDestroy
    public void destroy() {
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public DeploymentStatus doGetStatus(String deploymentId, boolean triggerEventIfUndeployed) {
        if (deploymentsMap.containsKey(deploymentId)) {
            return deploymentsMap.get(deploymentId);
        } else {
            Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
            if (deployment == null) {
                return DeploymentStatus.UNDEPLOYED;
            }
            QueryBuilder matchTopologyIdQueryBuilder = QueryBuilders.termQuery("topologyId", deploymentService.getTopologyIdByDeployment(deploymentId));
            final Application application = alienDAO.customFind(Application.class, matchTopologyIdQueryBuilder);
            if (application != null && UNKNOWN_APPLICATION_THAT_NEVER_WORKS.equals(application.getName())) {
                return DeploymentStatus.UNKNOWN;
            } else {
                // application is not deployed and but there is a deployment in alien so trigger the undeployed event to update status.
                if (triggerEventIfUndeployed) {
                    doChangeStatus(deploymentId, DeploymentStatus.UNDEPLOYED);
                }
                return DeploymentStatus.UNDEPLOYED;
            }
        }
    }

    private InstanceInformation newInstance(int i) {
        Map<String, String> properties = Maps.newHashMap();
        Map<String, String> attributes = Maps.newHashMap();
        attributes.put(PUBLIC_IP, "10.52.0." + i);
        attributes.put(TOSCA_ID, "1.0-wd03");
        attributes.put(TOSCA_NAME, "TOSCA-Simple-Profile-YAML");
        Map<String, String> runtimeProperties = Maps.newHashMap();
        runtimeProperties.put(PUBLIC_IP, "10.52.0." + i);
        return new InstanceInformation("init", InstanceStatus.PROCESSING, properties, attributes, runtimeProperties);
    }

    private ScalingPolicy getScalingPolicy(String id, Map<String, ScalingPolicy> policies, Map<String, NodeTemplate> nodeTemplates) {
        // Get the scaling of parent if not exist
        ScalingPolicy policy = policies.get(id);
        if (policy == null && nodeTemplates.get(id).getRelationships() != null) {
            for (RelationshipTemplate rel : nodeTemplates.get(id).getRelationships().values()) {
                if ("tosca.relationships.HostedOn".equals(rel.getType())) {
                    policy = getScalingPolicy(rel.getTarget(), policies, nodeTemplates);
                }
            }
        }
        return policy;
    }

    @Override
    protected synchronized void doDeploy(final String deploymentId) {
        final Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        log.info("Deploying deployment [" + deploymentId + "]");
        changeStatus(deploymentId, DeploymentStatus.DEPLOYMENT_IN_PROGRESS);
        if (deploymentId != null) {
            Topology topology = alienDAO.findById(Topology.class, deploymentService.getTopologyIdByDeployment(deploymentId));
            Map<String, ScalingPolicy> policies = topology.getScalingPolicies();
            if (policies == null) {
                policies = Maps.newHashMap();
            }
            Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
            if (nodeTemplates == null) {
                nodeTemplates = Maps.newHashMap();
            }
            Map<String, Map<String, InstanceInformation>> currentInformations = Maps.newHashMap();
            for (Map.Entry<String, NodeTemplate> nodeTemplateEntry : nodeTemplates.entrySet()) {
                Map<String, InstanceInformation> instanceInformations = Maps.newHashMap();
                currentInformations.put(nodeTemplateEntry.getKey(), instanceInformations);
                ScalingPolicy policy = getScalingPolicy(nodeTemplateEntry.getKey(), policies, nodeTemplates);
                int initialInstances = policy != null ? policy.getInitialInstances() : 1;
                for (int i = 1; i <= initialInstances; i++) {
                    InstanceInformation newInstanceInformation = newInstance(i);
                    instanceInformations.put(String.valueOf(i), newInstanceInformation);
                    notifyInstanceStateChanged(deploymentId, nodeTemplateEntry.getKey(), String.valueOf(i), newInstanceInformation, 1);
                }
            }
            instanceInformationsMap.put(deploymentId, currentInformations);
        }
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Application application = alienDAO.findById(Application.class, deployment.getSourceId());
                // To bluff the product owner, we must do as if it's deploying something, but in fact it's not
                if (application == null) {
                    changeStatus(deploymentId, DeploymentStatus.DEPLOYED);
                    return;
                }
                switch (application.getName()) {
                case BAD_APPLICATION_THAT_NEVER_WORKS:
                    changeStatus(deploymentId, DeploymentStatus.FAILURE);
                    break;
                case WARN_APPLICATION_THAT_NEVER_WORKS:
                    changeStatus(deploymentId, DeploymentStatus.WARNING);
                    break;
                default:
                    changeStatus(deploymentId, DeploymentStatus.DEPLOYED);
                }
            }

        }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected synchronized void doUndeploy(final String deploymentId) {
        log.info("Undeploying deployment [" + deploymentId + "]");
        changeStatus(deploymentId, DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS);
        if (instanceInformationsMap.containsKey(deploymentId)) {
            Map<String, Map<String, InstanceInformation>> appInfo = instanceInformationsMap.get(deploymentId);
            for (Map.Entry<String, Map<String, InstanceInformation>> nodeEntry : appInfo.entrySet()) {
                for (Map.Entry<String, InstanceInformation> instanceEntry : nodeEntry.getValue().entrySet()) {
                    instanceEntry.getValue().setState("stopping");
                    instanceEntry.getValue().setInstanceStatus(InstanceStatus.PROCESSING);
                    notifyInstanceStateChanged(deploymentId, nodeEntry.getKey(), instanceEntry.getKey(), instanceEntry.getValue(), 1);
                }
            }
        }
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                changeStatus(deploymentId, DeploymentStatus.UNDEPLOYED);
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    protected synchronized DeploymentStatus doChangeStatus(final String deploymentId, final DeploymentStatus status) {
        DeploymentStatus oldDeploymentStatus = deploymentsMap.put(deploymentId, status);
        if (oldDeploymentStatus == null) {
            oldDeploymentStatus = DeploymentStatus.UNDEPLOYED;
        }
        log.info("Deployment [" + deploymentId + "] pass from status [" + oldDeploymentStatus + "] to [" + status + "]");
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
                String cloudId = deployment.getCloudId();
                PaaSDeploymentStatusMonitorEvent event = new PaaSDeploymentStatusMonitorEvent();
                event.setDeploymentStatus(status);
                event.setDate((new Date()).getTime());
                event.setDeploymentId(deploymentId);
                event.setCloudId(cloudId);
                toBeDeliveredEvents.add(event);
                PaaSMessageMonitorEvent messageMonitorEvent = new PaaSMessageMonitorEvent();
                messageMonitorEvent.setDate((new Date()).getTime());
                messageMonitorEvent.setDeploymentId(deploymentId);
                messageMonitorEvent.setCloudId(cloudId);
                messageMonitorEvent.setMessage("APPLICATIONS.RUNTIME.EVENTS.MESSAGE_EVENT.STATUS_DEPLOYMENT_CHANGED");
                toBeDeliveredEvents.add(messageMonitorEvent);
            }
        }, 1, TimeUnit.SECONDS);
        return oldDeploymentStatus;
    }

    private void notifyInstanceStateChanged(final String deploymentId, final String nodeId, final String instanceId, final InstanceInformation information,
            long delay) {
        final InstanceInformation cloned = new InstanceInformation();
        cloned.setAttributes(information.getAttributes());
        cloned.setInstanceStatus(information.getInstanceStatus());
        cloned.setProperties(information.getProperties());
        cloned.setRuntimeProperties(information.getRuntimeProperties());
        cloned.setState(information.getState());
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
                String cloudId = deployment.getCloudId();
                PaaSInstanceStateMonitorEvent event;
                if (deployment.getSourceName().equals(BLOCKSTORAGE_APPLICATION) && cloned.getState().equalsIgnoreCase("created")) {
                    PaaSInstanceStorageMonitorEvent bsEvent = new PaaSInstanceStorageMonitorEvent();
                    bsEvent.setVolumeId(UUID.randomUUID().toString());
                    event = bsEvent;
                } else {
                    event = new PaaSInstanceStateMonitorEvent();
                }
                event.setInstanceId(instanceId.toString());
                event.setInstanceState(cloned.getState());
                event.setInstanceStatus(cloned.getInstanceStatus());
                event.setNodeTemplateId(nodeId);
                event.setDate((new Date()).getTime());
                event.setDeploymentId(deploymentId);
                event.setProperties(cloned.getProperties());
                event.setRuntimeProperties(cloned.getRuntimeProperties());
                event.setAttributes(cloned.getAttributes());
                event.setCloudId(cloudId);
                toBeDeliveredEvents.add(event);
                PaaSMessageMonitorEvent messageMonitorEvent = new PaaSMessageMonitorEvent();
                messageMonitorEvent.setDate((new Date()).getTime());
                messageMonitorEvent.setDeploymentId(deploymentId);
                messageMonitorEvent.setMessage("APPLICATIONS.RUNTIME.EVENTS.MESSAGE_EVENT.INSTANCE_STATE_CHANGED");
                messageMonitorEvent.setCloudId(cloudId);
                toBeDeliveredEvents.add(messageMonitorEvent);
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void notifyInstanceRemoved(final String deploymentId, final String nodeId, final String instanceId, long delay) {
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
                PaaSInstanceStateMonitorEvent event = new PaaSInstanceStateMonitorEvent();
                event.setInstanceId(instanceId.toString());
                event.setNodeTemplateId(nodeId);
                event.setDate((new Date()).getTime());
                event.setDeploymentId(deploymentId);
                event.setCloudId(deployment.getCloudId());
                toBeDeliveredEvents.add(event);
            }
        }, delay, TimeUnit.SECONDS);
    }

    private synchronized void doChangeInstanceInformations(String applicationId, Map<String, Map<String, InstanceInformation>> currentInformations) {
        Iterator<Entry<String, Map<String, InstanceInformation>>> appIterator = currentInformations.entrySet().iterator();
        while (appIterator.hasNext()) {
            Entry<String, Map<String, InstanceInformation>> iStatuses = appIterator.next();
            Iterator<Entry<String, InstanceInformation>> iterator = iStatuses.getValue().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, InstanceInformation> iStatus = iterator.next();
                changeInstanceState(applicationId, iStatuses.getKey(), iStatus.getKey(), iStatus.getValue(), iterator);
            }
            if (iStatuses.getValue().isEmpty()) {
                appIterator.remove();
            }
        }
    }

    private void changeInstanceState(String id, String nodeId, String instanceId, InstanceInformation information,
            Iterator<Entry<String, InstanceInformation>> iterator) {
        String currentState = information.getState();
        String nextState = getNextState(currentState);
        if (nextState != null) {
            information.setState(nextState);
            if ("started".equals(nextState)) {
                information.setInstanceStatus(InstanceStatus.SUCCESS);
            }
            if ("terminated".equals(nextState)) {
                iterator.remove();
                notifyInstanceRemoved(id, nodeId, instanceId, 2);
            } else {
                notifyInstanceStateChanged(id, nodeId, instanceId, information, 2);
            }
        }
    }

    private String getNextState(String currentState) {
        switch (currentState) {
        case "init":
            return "creating";
        case "creating":
            return "created";
        case "created":
            return "configuring";
        case "configuring":
            return "configured";
        case "configured":
            return "starting";
        case "starting":
            return "started";
        case "stopping":
            return "stopped";
        case "stopped":
            return "uninstalled";
        case "uninstalled":
            return "terminated";
        default:
            return null;
        }
    }

    private interface ScalingVisitor {
        void visit(String nodeTemplateId);
    }

    private void doScaledUpNode(ScalingVisitor scalingVisitor, String nodeTemplateId, Map<String, NodeTemplate> nodeTemplates) {
        scalingVisitor.visit(nodeTemplateId);
        for (Entry<String, NodeTemplate> nEntry : nodeTemplates.entrySet()) {
            if (nEntry.getValue().getRelationships() != null) {
                for (Entry<String, RelationshipTemplate> rt : nEntry.getValue().getRelationships().entrySet()) {
                    if (nodeTemplateId.equals(rt.getValue().getTarget()) && "tosca.relationships.HostedOn".equals(rt.getValue().getType())) {
                        doScaledUpNode(scalingVisitor, nEntry.getKey(), nodeTemplates);
                    }
                }
            }
        }
    }

    @Override
    public void scale(PaaSDeploymentContext deploymentContext, String nodeTemplateId, final int instances, IPaaSCallback<?> callback) {
        String deploymentId = deploymentContext.getDeploymentId();
        Deployment deployment = alienDAO.findById(Deployment.class, deploymentId);
        Topology topology = alienDAO.findById(Topology.class, deployment.getTopologyId());
        final Map<String, Map<String, InstanceInformation>> existingInformations = instanceInformationsMap.get(deploymentId);
        if (existingInformations != null && existingInformations.containsKey(nodeTemplateId)) {
            ScalingVisitor scalingVisitor = new ScalingVisitor() {
                @Override
                public void visit(String nodeTemplateId) {
                    Map<String, InstanceInformation> nodeInformations = existingInformations.get(nodeTemplateId);
                    if (nodeInformations != null) {
                        int currentSize = nodeInformations.size();
                        if (instances > 0) {
                            for (int i = currentSize + 1; i < currentSize + instances + 1; i++) {
                                nodeInformations.put(String.valueOf(i), newInstance(i));
                            }
                        } else {
                            for (int i = currentSize + instances + 1; i < currentSize + 1; i++) {
                                if (nodeInformations.containsKey(String.valueOf(i))) {
                                    nodeInformations.get(String.valueOf(i)).setState("stopping");
                                    nodeInformations.get(String.valueOf(i)).setInstanceStatus(InstanceStatus.PROCESSING);
                                }
                            }
                        }
                    }
                }
            };
            doScaledUpNode(scalingVisitor, nodeTemplateId, topology.getNodeTemplates());
        }
    }

    @Override
    public void getStatus(PaaSDeploymentContext deploymentContext, IPaaSCallback<DeploymentStatus> callback) {
        DeploymentStatus status = deploymentsMap.get(deploymentContext.getDeploymentId());
        status = status == null ? DeploymentStatus.UNDEPLOYED : status;
        callback.onSuccess(status);
    }

    @Override
    public void getInstancesInformation(PaaSDeploymentContext deploymentContext, Topology topology,
            IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback) {
        callback.onSuccess(instanceInformationsMap.get(deploymentContext.getDeploymentId()));
    }

    @Override
    public void getEventsSince(Date date, int maxEvents, IPaaSCallback<AbstractMonitorEvent[]> eventsCallback) {
        AbstractMonitorEvent[] events = toBeDeliveredEvents.toArray(new AbstractMonitorEvent[toBeDeliveredEvents.size()]);
        toBeDeliveredEvents.clear();
        eventsCallback.onSuccess(events);
    }

    @Override
    protected String doExecuteOperation(NodeOperationExecRequest request) {
        List<String> allowedOperation = Arrays.asList("updateWar", "updateWarFile", "addNode");
        String result = null;
        try {
            log.info("TRIGGERING OPERATION : {}", request.getOperationName());
            Thread.sleep(3000);
            log.info(" COMMAND REQUEST IS: " + JsonUtil.toString(request));
        } catch (JsonProcessingException | InterruptedException e) {
            log.error("OPERATION execution failled!", e);
            log.info("RESULT IS: KO");
            return "KO";
        }
        // only 2 operations in allowedOperation will return OK
        result = allowedOperation.contains(request.getOperationName()) ? "OK" : "KO";
        log.info("RESULT IS : {}", result);
        return result;
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyMap() {

        // Field 1 : managerUrl as string
        PropertyDefinition managerUrl = new PropertyDefinition();
        managerUrl.setType(ToscaType.STRING.toString());
        managerUrl.setRequired(true);
        managerUrl.setDescription("PaaS manager URL");
        managerUrl.setConstraints(null);
        PatternConstraint manageUrlConstraint = new PatternConstraint();
        manageUrlConstraint.setPattern("http://.+");
        managerUrl.setConstraints(Arrays.asList((PropertyConstraint) manageUrlConstraint));

        // Field 2 : number backup with constraint
        PropertyDefinition numberBackup = new PropertyDefinition();
        numberBackup.setType(ToscaType.INTEGER.toString());
        numberBackup.setRequired(true);
        numberBackup.setDescription("Number of backup");
        numberBackup.setConstraints(null);
        GreaterOrEqualConstraint greaterOrEqualConstraint = new GreaterOrEqualConstraint();
        greaterOrEqualConstraint.setGreaterOrEqual(String.valueOf("1"));
        numberBackup.setConstraints(Lists.newArrayList((PropertyConstraint) greaterOrEqualConstraint));

        // Field 3 : email manager
        PropertyDefinition managerEmail = new PropertyDefinition();
        managerEmail.setType(ToscaType.STRING.toString());
        managerEmail.setRequired(true);
        managerEmail.setDescription("PaaS manager email");
        managerEmail.setConstraints(null);
        PatternConstraint managerEmailConstraint = new PatternConstraint();
        managerEmailConstraint.setPattern(".+@.+");
        managerEmail.setConstraints(Arrays.asList((PropertyConstraint) managerEmailConstraint));

        deploymentProperties.put("managementUrl", managerUrl);
        deploymentProperties.put("numberBackup", numberBackup);
        deploymentProperties.put("managerEmail", managerEmail);

        return deploymentProperties;
    }

    @Override
    public ProviderConfig getDefaultConfiguration() {
        return null;
    }

    @Override
    public void setConfiguration(ProviderConfig configuration) throws PluginConfigurationException {
        log.info("In the plugin configurator <" + this.getClass().getName() + ">");
        try {
            log.info("The config object Tags is : {}", JsonUtil.toString(configuration.getTags()));
            log.info("The config object with error : {}", configuration.isWithBadConfiguraton());
            if (configuration.isWithBadConfiguraton()) {
                log.info("Throwing error for bad configuration");
                throw new PluginConfigurationException("Failed to configure Mock PaaS Provider Plugin error.");
            }
            this.providerConfiguration = configuration;
        } catch (JsonProcessingException e) {
            log.error("Fails to serialize configuration object as json string", e);
        }
    }

    @Override
    public String[] getAvailableResourceIds(CloudResourceType resourceType) {
        if (providerConfiguration != null && providerConfiguration.isProvideResourceIds()) {
            String[] ids = new String[10];
            for (int i = 0; i < 10; i++) {
                ids[i] = "yetAnotherResourceId-" + resourceType.name() + "-" + i;
            }
            return ids;
        } else {
            return null;
        }
    }

    @Override
    public String[] getAvailableResourceIds(CloudResourceType resourceType, String imageId) {
        return null;
    }
}
