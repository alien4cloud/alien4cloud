package org.alien4cloud.alm.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.alm.events.ManagedServiceResetEvent;
import org.alien4cloud.alm.events.ManagedServiceUpdatedEvent;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.events.DeploymentCreatedEvent;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.orchestrators.locations.events.OnLocationResourceChangeEvent;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.IPaasEventListener;
import alien4cloud.paas.IPaasEventService;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.utils.PropertyUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * This service is responsible for performing the link between an alien deployment and it's service representation.
 *
 * It performs service update based on deployment events.
 */
@Service
@Slf4j
public class ManagedServiceResourceEventService implements IPaasEventListener<AbstractMonitorEvent> {
    @Inject
    private ApplicationEventPublisher publisher;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private DeploymentService deploymentService;
    @Resource
    private IPaasEventService paasEventService;


    @Inject
    private ManagedServiceResourceService managedServiceResourceService;

    @PostConstruct
    public void register() {
        paasEventService.addListener(this);
    }

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
            resetRunningServiceResource(serviceResource);
        } else {
            // Set service node attribute and properties.
            updateRunningService(serviceResource, event.getDeploymentId(), state);
        }
    }

    @Override
    public boolean canHandle(AbstractMonitorEvent event) {
        return event instanceof PaaSDeploymentStatusMonitorEvent;
        // TODO support live attribute changes through || event instanceof PaaSInstanceStateMonitorEvent;
    }

    private void updateRunningService(ServiceResource serviceResource, String deploymentId, String state) {
        Deployment deployment = deploymentService.get(deploymentId);
        if (deployment == null) {
            log.error("Unable to update service [ {} ] as deployment [ {} ] cannot be found.", serviceResource.getId(), deploymentId);
            resetRunningServiceResource(serviceResource);
            return;
        }
        DeploymentTopology topology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
        updateRunningService(topology, serviceResource, deployment, state);
    }

    /**
     * Asynchronously update a service.
     *
     * @param topology The deployment topology that describe the service.
     * @param serviceResource The service resource.
     * @param deployment The deployment that is linked to the service.
     * @param serviceState The current state of the service.
     */
    public void updateRunningService(final DeploymentTopology topology, final ServiceResource serviceResource, final Deployment deployment,
            final String serviceState) {
        // we need to fetch the instances and build the Service Resource instance out of it.
        deploymentRuntimeStateService.getInstancesInformation(deployment, new IPaaSCallback<Map<String, Map<String, InstanceInformation>>>() {
            @Override
            public void onSuccess(Map<String, Map<String, InstanceInformation>> instanceInformation) {
                updateRunningService(topology, deployment, serviceResource, serviceState, instanceInformation);
            }

            @Override
            public void onFailure(Throwable throwable) {
                // Actually we should retry later on
                log.error("Failed to update running service");
            }
        });
    }

    private void updateRunningService(DeploymentTopology topology, Deployment deployment, ServiceResource serviceResource, String serviceState,
            Map<String, Map<String, InstanceInformation>> instanceInformation) {

        // update the state
        serviceResource.setState(serviceState);

        // update deploymentId, in case it is not yet (when creating the service from an already started deployment)
        serviceResource.setDeploymentId(deployment.getId());

        // ensure the service is available on all of the deployment locations
        updateLocations(serviceResource, deployment.getLocationIds());

        // Map input properties from the topology as properties of the service instance
        if (serviceResource.getNodeInstance().getNodeTemplate().getProperties() == null) {
            serviceResource.getNodeInstance().getNodeTemplate().setProperties(Maps.newHashMap());
        }
        serviceResource.getNodeInstance().getNodeTemplate().getProperties().putAll(safe(topology.getAllInputProperties()));

        // nodeName -> { attributeName -> [ att aliases ] }
        Map<String, Map<String, Set<String>>> attributeAliases = Maps.newHashMap();
        topology.getNodeTemplates().forEach((nodeName, nodeTemplate) -> {
            String exposedAttAliases = TopologyModifierSupport.getNodeTagValueOrNull(nodeTemplate, TopologyModifierSupport.A4C_MODIFIER_TAG_EXPOSED_ATTRIBUTE_ALIAS);
            if (exposedAttAliases != null) {
                Map<String, Set<String>> nodeAttributeAliases = attributeAliases.get(nodeName);
                if (nodeAttributeAliases == null) {
                    nodeAttributeAliases = Maps.newHashMap();
                    attributeAliases.put(nodeName, nodeAttributeAliases);
                }
                // aa:bb
                // attributeSourceA:attributeAlias1,attributeSourceA:attributeAlias2,attributeSourceB:attributeAlias3
                String[] tuples = exposedAttAliases.split(",");
                for (String tuple: tuples) {
                    int separatorIndex = tuple.indexOf(":");
                    if (separatorIndex > -1) {
                        String attributeName = tuple.substring(0, separatorIndex);
                        String aliasName = tuple.substring(separatorIndex + 1);
                        Set<String> aliases = nodeAttributeAliases.get(attributeName);
                        if (aliases == null) {
                            aliases = Sets.newHashSet();
                            nodeAttributeAliases.putIfAbsent(attributeName, aliases);
                        }
                        aliases.add(aliasName);
                    }
                }
            }
        });

        // Map attributes from the instances to the actual service resource node.
        for (Entry<String, Set<String>> nodeOutputAttrEntry : safe(topology.getOutputAttributes()).entrySet()) {
            Map<String, InstanceInformation> instances = instanceInformation.get(nodeOutputAttrEntry.getKey());
            if (instances == null) {
                log.error("Failed to map attributes from node [ {} ] for service <id: {}, name: {}>. The node cannot be found in deployed topology [ {} ].",
                        nodeOutputAttrEntry.getKey(), serviceResource.getId(), serviceResource.getName(), topology.getId());
            } else if (instances.size() > 1) {
                log.error("Services substitution does not yet supports the exposure of multiple instances");
            } else {
                Entry<String, InstanceInformation> instance = instances.entrySet().iterator().next();
//                InstanceInformation instance = instances.values().iterator().next();
                // let's map attribute
                for (String mappedAttribute : nodeOutputAttrEntry.getValue()) {
                    String nodeName = instance.getValue().getAttributes().get("tosca_name");
                    String attributeValue = instance.getValue().getAttributes().get(mappedAttribute);
                    serviceResource.getNodeInstance().setAttribute(mappedAttribute, attributeValue);
                    Map<String, Set<String>> nodeAliases = attributeAliases.get(nodeName);
                    if (nodeAliases != null) {
                        Set<String> nodeAttributeAliases = nodeAliases.get(mappedAttribute);
                        if (nodeAttributeAliases != null) {
                            for (String nodeAttributeAliase : nodeAttributeAliases) {
                                serviceResource.getNodeInstance().setAttribute(nodeAttributeAliase, attributeValue);
                            }
                        }
                    }
                }
            }
        }

        // Map output properties as attributes of the service instance
        for (Entry<String, Set<String>> nodeOutputPropEntry : safe(topology.getOutputProperties()).entrySet()) {
            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeOutputPropEntry.getKey());
            for (String prop : nodeOutputPropEntry.getValue()) {
                serviceResource.getNodeInstance().setAttribute(prop, PropertyUtil.serializePropertyValue(nodeTemplate.getProperties().get(prop)));
            }
        }

        // Map capabilities output properties as attributes of the service instance (that are exposed as node properties)
        for (Entry<String, Map<String, Set<String>>> nodeOutputCapaPropEntry : safe(topology.getOutputCapabilityProperties()).entrySet()) {
            NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeOutputCapaPropEntry.getKey());
            for (Entry<String, Set<String>> outputCapaPropEntry : nodeOutputCapaPropEntry.getValue().entrySet()) {
                Capability capability = nodeTemplate.getCapabilities().get(outputCapaPropEntry.getKey());
                for (String prop : outputCapaPropEntry.getValue()) {
                    serviceResource.getNodeInstance().setAttribute(prop, PropertyUtil.serializePropertyValue(capability.getProperties().get(prop)));
                }
            }
        }

        serviceResource.getNodeInstance().getNodeTemplate().setCapabilities(Maps.newLinkedHashMap());
        // Map capabilities exposed as is for the service node.
        for (Entry<String, SubstitutionTarget> capabilityMapping : safe(topology.getSubstitutionMapping().getCapabilities()).entrySet()) {
            Capability deployedCapability = topology.getNodeTemplates().get(capabilityMapping.getValue().getNodeTemplateName()).getCapabilities()
                    .get(capabilityMapping.getValue().getTargetId());
            serviceResource.getNodeInstance().getNodeTemplate().getCapabilities().put(capabilityMapping.getKey(), deployedCapability);
            // TODO improve while capabilities attributes will be really supported
            // Workaround to support capabilities attributes is to use node attributes with keys in format capabilities.capaName.attributeName
            mapCapabilityRequirementAttributes(serviceResource, instanceInformation, capabilityMapping.getValue().getNodeTemplateName(), "capabilities",
                    capabilityMapping.getValue().getTargetId());
        }

        serviceResource.getNodeInstance().getNodeTemplate().setRequirements(Maps.newLinkedHashMap());
        // Map requirements exposed as is for the service node.
        for (Entry<String, SubstitutionTarget> requirementMapping : safe(topology.getSubstitutionMapping().getRequirements()).entrySet()) {
            serviceResource.getNodeInstance().getNodeTemplate().getRequirements().put(requirementMapping.getKey(), topology.getNodeTemplates()
                    .get(requirementMapping.getValue().getNodeTemplateName()).getRequirements().get(requirementMapping.getValue().getTargetId()));
            // TODO improve while requirements attributes will be really supported
            // Workaround to support requirements attributes is to use node attributes with keys in format capabilities.capaName.attributeName
            mapCapabilityRequirementAttributes(serviceResource, instanceInformation, requirementMapping.getValue().getNodeTemplateName(), "requirements",
                    requirementMapping.getValue().getTargetId());
        }

        serviceResourceService.save(serviceResource);

        // trigger a ManagedServiceUpdateEvent
        publisher.publishEvent(new ManagedServiceUpdatedEvent(this, serviceResource, topology));
    }

    private void mapCapabilityRequirementAttributes(ServiceResource serviceResource, Map<String, Map<String, InstanceInformation>> instanceInformation,
            String nodeTemplateName, String prefix, String capaReqName) {
        Map<String, InstanceInformation> instances = instanceInformation.get(nodeTemplateName);
        if (instances == null) {
            log.error("Failed to map attributes from node [ {} ] capability for service <id: {}, name: {}>. The node cannot be found in deployed topology.",
                    nodeTemplateName, serviceResource.getId(), serviceResource.getName());
        } else if (instances.size() > 1) {
            log.error("Services substitution does not yet supports the exposure of multiple instances");
        } else {
            InstanceInformation instance = instances.values().iterator().next();
            // Map attributes that belongs the exposed capability
            String attributePrefix = prefix + "." + capaReqName;
            // TODO filter to avoid display of properties values here.
            // String newPrefix = "capabilities." + capabilityMapping.getKey();
            for (Entry<String, String> attributeEntry : instance.getAttributes().entrySet()) {
                if (attributeEntry.getKey().startsWith(attributePrefix)) {
                    // String newKey = newPrefix + attributeEntry.getKey().substring(attributePrefix.length());
                    serviceResource.getNodeInstance().setAttribute(attributeEntry.getKey(), attributeEntry.getValue());
                }
            }
        }
    }

    /**
     * add if not yet present the given locations ids into the serviceResource
     * 
     * @param serviceResource
     * @param locationsToAdd
     */
    private void updateLocations(ServiceResource serviceResource, String[] locationsToAdd) {
        if (serviceResource.getLocationIds() == null) {
            serviceResource.setLocationIds(locationsToAdd);
            for (String locationId : serviceResource.getLocationIds()) {
                publisher.publishEvent(new OnLocationResourceChangeEvent(this, locationId));
            }
        } else {
            Set<String> locations = Sets.newHashSet(serviceResource.getLocationIds());
            locations.addAll(Sets.newHashSet(locationsToAdd));
            serviceResource.setLocationIds(locations.toArray(new String[locations.size()]));
        }
        for (String locationId : locationsToAdd) {
            publisher.publishEvent(new OnLocationResourceChangeEvent(this, locationId));
        }
    }

    /**
     * Reset the service resource, by cleaning everything related to runtime
     * 
     * @param serviceResource
     */
    public void resetRunningServiceResource(ServiceResource serviceResource) {
        serviceResource.getNodeInstance().setAttributeValues(Maps.newHashMap());
        serviceResource.setDeploymentId(null);
        serviceResource.setState(ToscaNodeLifecycleConstants.INITIAL);
        serviceResourceService.save(serviceResource);
        // trigger a ManagedServiceServiceReset
        publisher.publishEvent(new ManagedServiceResetEvent(this, serviceResource));
    }

    /**
     * On {@link DeploymentCreatedEvent}, eventually update the linked managed service
     * 
     * @param event
     */
    @EventListener
    public void onDeploymentCreatedEvent(DeploymentCreatedEvent event) {
        Deployment deployment = deploymentService.get(event.getDeploymentId());
        if (deployment != null) {
            ServiceResource serviceResource = managedServiceResourceService.get(deployment.getEnvironmentId());
            if (serviceResource != null) {
                serviceResource.setDeploymentId(event.getDeploymentId());

                serviceResourceService.save(serviceResource);
            }
        }
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
        case UPDATED:
            return ToscaNodeLifecycleConstants.STARTED;
        case FAILURE:
            return ToscaNodeLifecycleConstants.ERROR;
        case UNDEPLOYED:
            return ToscaNodeLifecycleConstants.INITIAL;
        case INIT_DEPLOYMENT:
        case DEPLOYMENT_IN_PROGRESS:
        case UNDEPLOYMENT_IN_PROGRESS:
        case UPDATE_FAILURE:
        case UPDATE_IN_PROGRESS:
        case WARNING:
        case UNKNOWN:
            break;
        }
        return null;
    }
}
