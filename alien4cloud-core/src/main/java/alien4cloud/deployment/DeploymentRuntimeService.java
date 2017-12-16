package alien4cloud.deployment;

import java.util.Map;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.secret.services.SecretProviderService;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.normative.constants.AlienCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.AlienInterfaceTypes;
import org.alien4cloud.tosca.normative.constants.NormativeCapabilityTypes;
import org.alien4cloud.tosca.normative.constants.NormativeComputeConstants;
import org.alien4cloud.tosca.utils.NodeTemplateUtils;
import org.alien4cloud.tosca.utils.TopologyUtils;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.matching.services.location.TopologyLocationUtils;
import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.exception.MaintenanceModeException;
import alien4cloud.paas.exception.OperationExecutionException;
import alien4cloud.paas.exception.OrchestratorDisabledException;
import alien4cloud.paas.model.NodeOperationExecRequest;
import alien4cloud.paas.model.OperationExecRequest;
import alien4cloud.paas.model.PaaSDeploymentContext;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.tosca.context.ToscaContextualAspect;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages operations performed on a running deployment.
 */
@Slf4j
@Service
public class DeploymentRuntimeService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Inject
    private DeploymentContextService deploymentContextService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentTopologyService deploymentTopologyService;
    @Inject
    private ToscaContextualAspect toscaContextualAspect;
    @Inject
    private DeployService deployService;
    @Inject
    private SecretProviderService secretProviderService;

    /**
     * Build the deployment context from an operation execution request
     * 
     * @param request the operation execution request
     * @return the deployment context
     */
    public PaaSTopologyDeploymentContext buildPaaSTopologyDeploymentContext(OperationExecRequest request) {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(request.getApplicationEnvironmentId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(deploymentTopology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = secretProviderService.generateSecretConfiguration(locations,
                request.getSecretProviderPluginName(), request.getSecretProviderCredentials());
        return deploymentContextService.buildTopologyDeploymentContext(secretProviderConfigurationAndCredentials, deployment,
                deploymentTopologyService.getLocations(deploymentTopology), deploymentTopology);
    }

    /**
     * Trigger the execution of an operation on a node.
     *
     * @param request the operation's execution description ( see {@link alien4cloud.paas.model.OperationExecRequest})
     * @param callback the callback when execution finishes
     * @throws alien4cloud.paas.exception.OperationExecutionException runtime exception during an operation
     */
    public void triggerOperationExecution(OperationExecRequest request, IPaaSCallback<Map<String, String>> callback) throws OperationExecutionException {
        PaaSTopologyDeploymentContext context = buildPaaSTopologyDeploymentContext(request);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(context.getDeployment().getOrchestratorId());
        orchestratorPlugin.executeOperation(context, request, callback);
    }

    public void triggerOperationExecution(PaaSTopologyDeploymentContext context, OperationExecRequest request, IPaaSCallback<Map<String, String>> callback)
            throws OperationExecutionException {
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(context.getDeployment().getOrchestratorId());
        orchestratorPlugin.executeOperation(context, request, callback);
    }

    /**
     * Switch an instance in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param nodeTemplateId The id of the node template on which to enable maintenance mode.
     * @param instanceId The id of the instance.
     * @param maintenanceModeOn true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchInstanceMaintenanceMode(String applicationEnvironmentId, String nodeTemplateId, String instanceId, boolean maintenanceModeOn)
            throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, null);
        orchestratorPlugin.switchInstanceMaintenanceMode(deploymentContext, nodeTemplateId, instanceId, maintenanceModeOn);
    }

    /**
     * Switch all instances in a deployment to maintenance mode. If so the orchestrator should not perform self healing operations for this instance.
     *
     * @param applicationEnvironmentId The id of the application environment.
     * @param maintenanceModeOn true if we should enable the maintenance mode, false if we should disable it.
     * @throws MaintenanceModeException In case the operation fails.
     */
    public void switchMaintenanceMode(String applicationEnvironmentId, boolean maintenanceModeOn) throws MaintenanceModeException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        DeploymentTopology deploymentTopology = deploymentRuntimeStateService.getRuntimeTopologyFromEnvironment(deployment.getEnvironmentId());
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, deploymentTopology, null);
        orchestratorPlugin.switchMaintenanceMode(deploymentContext, maintenanceModeOn);
    }

    /**
     * Scale up/down a node in a topology
     *
     * @param applicationEnvironmentId id of the targeted environment
     * @param nodeTemplateId id of the compute node to scale up
     * @param instances the number of instances to be added (if positive) or removed (if negative)
     */
    public void scale(SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials, String applicationEnvironmentId,
            final String nodeTemplateId, int instances, final IPaaSCallback<Object> callback) throws OrchestratorDisabledException {
        Deployment deployment = deploymentService.getActiveDeploymentOrFail(applicationEnvironmentId);
        final DeploymentTopology topology = alienMonitorDao.findById(DeploymentTopology.class, deployment.getId());
        toscaContextualAspect.execInToscaContext(() -> {
            doScale(nodeTemplateId, instances, callback, deployment, topology, secretProviderConfigurationAndCredentials);
            return null;
        }, false, topology);
    }

    private void doScale(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback, final Deployment deployment,
            final DeploymentTopology topology, SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(topology, nodeTemplateId);

        // get the secret provider configuration from the location
        Map<String, String> locationIds = TopologyLocationUtils.getLocationIds(topology);
        Map<String, Location> locations = deploymentTopologyService.getLocations(locationIds);
        secretProviderConfigurationAndCredentials = secretProviderService.generateSecretConfiguration(locations,
                secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().getPluginName(),
                secretProviderConfigurationAndCredentials.getCredentials());

        // Get alien4cloud specific interface to support cluster controller nodes.
        Capability clusterControllerCapability = NodeTemplateUtils.getCapabilityByType(nodeTemplate, AlienCapabilityTypes.CLUSTER_CONTROLLER);
        if (clusterControllerCapability == null) {
            doScaleNode(nodeTemplateId, instances, callback, deployment, topology, nodeTemplate, secretProviderConfigurationAndCredentials);
        } else {
            triggerClusterManagerScaleOperation(nodeTemplateId, instances, callback, deployment, topology, clusterControllerCapability,
                    secretProviderConfigurationAndCredentials);
        }
    }

    private void triggerClusterManagerScaleOperation(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback,
            final Deployment deployment, final DeploymentTopology topology, Capability clusterControllerCapability,
            SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        NodeOperationExecRequest scaleOperationRequest = new NodeOperationExecRequest();
        // Instance id is not specified for cluster control nodes
        scaleOperationRequest.setNodeTemplateName(nodeTemplateId);
        scaleOperationRequest.setInterfaceName(AlienInterfaceTypes.CLUSTER_CONTROL);
        scaleOperationRequest.setOperationName(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE);

        int currentInstances = TopologyUtils.getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, clusterControllerCapability);
        int expectedInstances = currentInstances + instances;
        log.info("Scaling [ {} ] node from [ {} ] to [ {} ]. Updating runtime topology...", nodeTemplateId, currentInstances, expectedInstances);
        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, expectedInstances, clusterControllerCapability);
        alienMonitorDao.save(topology);

        scaleOperationRequest.setParameters(Maps.newHashMap());
        scaleOperationRequest.getParameters().put(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE_PARAMS_INSTANCES_DELTA, String.valueOf(instances));
        scaleOperationRequest.getParameters().put(AlienInterfaceTypes.CLUSTER_CONTROL_OP_SCALE_PARAMS_EXPECTED_INSTANCES, String.valueOf(expectedInstances));

        orchestratorPlugin.executeOperation(deploymentContextService.buildTopologyDeploymentContext(secretProviderConfigurationAndCredentials, deployment,
                deploymentTopologyService.getLocations(topology), topology), scaleOperationRequest, new IPaaSCallback<Map<String, String>>() {
                    @Override
                    public void onSuccess(Map<String, String> data) {
                        callback.onSuccess(data);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        log.info("Failed to scale [ {} ] node from [ {} ] to [ {} ]. rolling back to {}...", nodeTemplateId, currentInstances,
                                expectedInstances, currentInstances);
                        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, currentInstances, clusterControllerCapability);
                        alienMonitorDao.save(topology);
                        callback.onFailure(throwable);
                    }
                });
    }

    private void doScaleNode(final String nodeTemplateId, final int instances, final IPaaSCallback<Object> callback, final Deployment deployment,
            final DeploymentTopology topology, NodeTemplate nodeTemplate, SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials) {
        final Capability capability = NodeTemplateUtils.getCapabilityByTypeOrFail(nodeTemplate, NormativeCapabilityTypes.SCALABLE);
        final int previousInitialInstances = TopologyUtils.getScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, capability);
        final int newInitialInstances = previousInitialInstances + instances;
        log.info("Scaling [ {} ] node from [ {} ] to [ {} ]. Updating runtime topology...", nodeTemplateId, previousInitialInstances, newInitialInstances);
        TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, newInitialInstances, capability);
        alienMonitorDao.save(topology);

        IOrchestratorPlugin orchestratorPlugin = orchestratorPluginService.getOrFail(deployment.getOrchestratorId());
        PaaSDeploymentContext deploymentContext = new PaaSDeploymentContext(deployment, topology, secretProviderConfigurationAndCredentials);
        orchestratorPlugin.scale(deploymentContext, nodeTemplateId, instances, new IPaaSCallback() {
            @Override
            public void onFailure(Throwable throwable) {
                log.info("Failed to scale [ {} ] node from [ {} ] to [ {} ]. rolling back to {}...", nodeTemplateId, previousInitialInstances,
                        newInitialInstances, previousInitialInstances);
                TopologyUtils.setScalingProperty(NormativeComputeConstants.SCALABLE_DEFAULT_INSTANCES, previousInitialInstances, capability);
                alienMonitorDao.save(topology);
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(Object data) {
                callback.onSuccess(data);
            }
        });
    }
}