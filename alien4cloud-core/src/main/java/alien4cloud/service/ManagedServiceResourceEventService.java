package alien4cloud.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * This service is responsible for performing the link between an alien deployment and it's service representation.
 *
 * It performs service update based on deployment events.
 */
@Service
@Slf4j
public class ManagedServiceResourceEventService implements IPaasEventListener<AbstractMonitorEvent> {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentService deploymentService;

    @Override
    public void eventHappened(AbstractMonitorEvent event) {
        String state = getInstanceStateFromDeploymentStatus(((PaaSDeploymentStatusMonitorEvent) event).getDeploymentStatus());
        if (state == null) { // Ignored.
            return;
        }

        ServiceResource serviceResource = alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("deploymentId", event.getDeploymentId()))
                .prepareSearch().find();
        if (serviceResource == null) { // No service resources matching this deployment.
            return;
        }

        String currentState = serviceResource.getNodeInstance().getAttributeValues().get(ToscaNodeLifecycleConstants.ATT_STATE);
        if (state.equals(currentState)) {
            return; // nothing changed
        }
        if (ToscaNodeLifecycleConstants.INITIAL.equals(state)) {
            // Reset service node attribute and properties.
            serviceResource.getNodeInstance().setAttributeValues(Maps.newHashMap());
            // TODO Dispatch on service changed event.
            serviceResourceService.save(serviceResource, false);
        } else if (ToscaNodeLifecycleConstants.STARTED.equals(state)) {
            // Set service node attribute and properties.
            updateRunningService(serviceResource, event.getDeploymentId(), state);
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSDeploymentStatusMonitorEvent;
        // TODO support live attribute changes through || event instanceof PaaSInstanceStateMonitorEvent;
    }

    private void updateRunningService(ServiceResource resource, String deploymentId, String state) {
        Deployment deployment = deploymentService.get(deploymentId);
        if (deployment == null) {
            log.error("Unable to update service <{}> as deployment <{}> cannot be found.", resource.getId(), deploymentId);
            return;
        }
        Topology topology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
        updateRunningService(topology, resource, deployment, state);
    }

    /**
     * Asynchronously update a service.
     *
     * @param topology The deployment topology that describe the service.
     * @param resource The service resource.
     * @param deployment The deployment that is linked to the service.
     * @param serviceState The current state of the service.
     */
    public void updateRunningService(final Topology topology, final ServiceResource resource, final Deployment deployment, final String serviceState) {
        // we need to fetch the instances and build the Service Resource instance out of it.
        deploymentRuntimeStateService.getInstancesInformation(deployment, new IPaaSCallback<Map<String, Map<String, InstanceInformation>>>() {
            @Override
            public void onSuccess(Map<String, Map<String, InstanceInformation>> instanceInformation) {
                updateRunningService(topology, resource, serviceState, instanceInformation);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // Actually we should retry later on
                log.error("Failed to update running service");
            }
        });
    }

    private void updateRunningService(Topology topology, ServiceResource resource, String serviceState,
            Map<String, Map<String, InstanceInformation>> instanceInformation) {
        // Map attributes from the instances to the actual service resource node.
        for (Entry<String, Set<String>> nodeOutputAttrEntry : safe(topology.getOutputAttributes()).entrySet()) {
            Map<String, InstanceInformation> instances = instanceInformation.get(nodeOutputAttrEntry.getKey());
            if (instances == null) {
                log.error("Failed to map attributes from node <{}> for service <id: {}, name: {}>. The node cannot be found in deployed topology <{}>.",
                        nodeOutputAttrEntry.getKey(), resource.getId(), resource.getName(), topology.getId());
            } else if (instances.size() > 1) {
                log.error("Services substitution does not yet supports the exposure of multiple instances");
            } else {
                InstanceInformation instance = instances.values().iterator().next();
                // let's map attribute
                for (String mappedAttribute : nodeOutputAttrEntry.getValue()) {
                    resource.getNodeInstance().setAttribute(mappedAttribute, instance.getAttributes().get(mappedAttribute));
                }
            }
        }

        // Map properties
        resource.getNodeInstance().getNodeTemplate().setProperties(Maps.newLinkedHashMap());
        for (Entry<String, Set<String>> nodeOutputPropEntry : safe(topology.getOutputProperties()).entrySet()) {
            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeOutputPropEntry.getKey());
            for (String prop : nodeOutputPropEntry.getValue()) {
                resource.getNodeInstance().getNodeTemplate().getProperties().put(prop, nodeTemplate.getProperties().get(prop));
            }
        }

        // Map properties out of capabilities (that are exposed as node properties)
        for (Entry<String, Map<String, Set<String>>> nodeOutputCapaPropEntry : safe(topology.getOutputCapabilityProperties()).entrySet()) {
            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeOutputCapaPropEntry.getKey());
            for (Entry<String, Set<String>> outputCapaPropEntry : nodeOutputCapaPropEntry.getValue().entrySet()) {
                Capability capability = nodeTemplate.getCapabilities().get(outputCapaPropEntry.getKey());
                for (String prop : outputCapaPropEntry.getValue()) {
                    resource.getNodeInstance().getNodeTemplate().getProperties().put(prop, capability.getProperties().get(prop));
                }
            }
        }

        resource.getNodeInstance().getNodeTemplate().setCapabilities(Maps.newLinkedHashMap());
        // Map capabilities exposed as is for the service node.
        for (Entry<String, SubstitutionTarget> capabilityMapping : topology.getSubstitutionMapping().getCapabilities().entrySet()) {
            resource.getNodeInstance().getNodeTemplate().getCapabilities().put(capabilityMapping.getKey(), topology.getNodeTemplates()
                    .get(capabilityMapping.getValue().getNodeTemplateName()).getCapabilities().get(capabilityMapping.getValue().getTargetId()));
        }

        resource.getNodeInstance().getNodeTemplate().setRequirements(Maps.newLinkedHashMap());
        // Map requirements exposed as is for the service node.
        for (Entry<String, SubstitutionTarget> requirementMapping : topology.getSubstitutionMapping().getRequirements().entrySet()) {
            resource.getNodeInstance().getNodeTemplate().getRequirements().put(requirementMapping.getKey(), topology.getNodeTemplates()
                    .get(requirementMapping.getValue().getNodeTemplateName()).getRequirements().get(requirementMapping.getValue().getTargetId()));
        }

        serviceResourceService.save(resource, false);
    }

    /**
     * Utility method
     *
     * @param deploymentStatus
     * @return
     */
    public String getInstanceStateFromDeploymentStatus(DeploymentStatus deploymentStatus) {
        switch (deploymentStatus) {
        case DEPLOYED:
            return ToscaNodeLifecycleConstants.STARTED;
        case FAILURE:
            return ToscaNodeLifecycleConstants.ERROR;
        case UNDEPLOYED:
            return ToscaNodeLifecycleConstants.INITIAL;
        case INIT_DEPLOYMENT:
        case DEPLOYMENT_IN_PROGRESS:
        case UNDEPLOYMENT_IN_PROGRESS:
        case WARNING:
        case UNKNOWN:
            break;
        }
        return null;
    }
}
