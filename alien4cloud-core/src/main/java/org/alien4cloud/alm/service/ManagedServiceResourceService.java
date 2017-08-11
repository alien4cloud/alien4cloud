package org.alien4cloud.alm.service;

import static alien4cloud.dao.FilterUtil.fromKeyValueCouples;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.alm.events.ManagedServiceCreatedEvent;
import org.alien4cloud.alm.events.ManagedServiceDeletedEvent;
import org.alien4cloud.alm.events.ManagedServiceUnbindEvent;
import org.alien4cloud.alm.service.exceptions.InvalidDeploymentStatusException;
import org.alien4cloud.alm.service.exceptions.MissingSubstitutionException;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.editor.events.SubstitutionTypeChangedEvent;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationService;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.deployment.DeploymentRuntimeStateService;
import alien4cloud.deployment.DeploymentService;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.Application;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.deployment.Deployment;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.security.AuthorizationUtil;
import alien4cloud.topology.TopologyServiceCore;
import lombok.extern.slf4j.Slf4j;

/**
 * This service handles the service resources managed by alien4cloud through deployments.
 */
@Slf4j
@Service
public class ManagedServiceResourceService {
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Inject
    private ApplicationService applicationService;
    @Inject
    private ApplicationEnvironmentService environmentService;
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private NodeInstanceService nodeInstanceService;
    @Inject
    private ManagedServiceResourceEventService managedServiceResourceEventService;
    @Inject
    private TopologyServiceCore topologyServiceCore;
    @Inject
    private DeploymentService deploymentService;
    @Inject
    private DeploymentRuntimeStateService deploymentRuntimeStateService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;
    @Inject
    private ApplicationEventPublisher publisher;

    /**
     * Create a Service resource associated with the given environment.
     * 
     * @param environmentId The environment to create a service for, the service version will be the one of the environment current associated version.
     * @param serviceName The name of the service as it should appears.
     * @param fromRuntime If we should try to create the service from the runtime topology related to the environment.
     * @return the id of the created service
     *
     * @throws AlreadyExistException if a service with the given name, or related to the given environment already exists
     * @throws alien4cloud.exception.NotFoundException if <b>fromRuntime</b> is set to true, but the environment is not deployed
     * @throws MissingSubstitutionException if topology related to the environment doesn't define a substitution type
     */
    public synchronized String create(String serviceName, String environmentId, boolean fromRuntime) {
        ApplicationEnvironment environment = checkAndGetApplicationEnvironment(environmentId);

        // check that the service does not exists already for this environment
        if (alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).count() > 0) {
            throw new AlreadyExistException(
                    "A service resource for environment <" + environmentId + "> and version <" + environment.getTopologyVersion() + "> already exists.");
        }

        Topology topology;
        String state = ToscaNodeLifecycleConstants.INITIAL;
        Deployment deployment = null;
        if (fromRuntime) {
            deployment = deploymentService.getActiveDeploymentOrFail(environmentId);
            topology = deploymentRuntimeStateService.getRuntimeTopology(deployment.getId());
            DeploymentStatus currentStatus = deploymentRuntimeStateService.getDeploymentStatus(deployment);
            state = managedServiceResourceEventService.getInstanceStateFromDeploymentStatus(currentStatus);
            if (state == null) { // We need a valid deployment state to expose as service.
                throw new InvalidDeploymentStatusException(
                        "Creating a service out of a running deployment is possible only when it's status is one of [DEPLOYED, FAILURE, UNDEPLOYED] current was <"
                                + currentStatus + ">",
                        currentStatus);
            }
        } else {
            topology = topologyServiceCore.getOrFail(Csar.createId(environment.getApplicationId(), environment.getTopologyVersion()));
        }

        if (topology.getSubstitutionMapping() == null) {
            throw new MissingSubstitutionException("Substitution is required to expose a topology.");
        }

        // The elementId of the type created out of the substitution is currently the archive name.
        String serviceId = serviceResourceService.create(serviceName, environment.getTopologyVersion(), topology.getArchiveName(),
                environment.getTopologyVersion(), environmentId);

        // Update the service relationships definition from the topology substitution
        updateServiceRelationship(serviceId, topology);

        if (fromRuntime) {
            managedServiceResourceEventService.updateRunningService((DeploymentTopology) topology, serviceResourceService.getOrFail(serviceId), deployment,
                    state);
        }
        ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);

        // trigger a ManagedServiceCreatedEvent
        publisher.publishEvent(new ManagedServiceCreatedEvent(this, serviceResource));

        return serviceId;
    }

    /**
     * Get the service resource associated with an environment.
     * 
     * @param environmentId The environment for which to get the service resource.
     * @return A service resource instance if there is one associated with the environment or null if not.
     */
    public ServiceResource get(String environmentId) {
        checkAndGetApplicationEnvironment(environmentId);
        return alienDAO.buildQuery(ServiceResource.class).setFilters(fromKeyValueCouples("environmentId", environmentId)).prepareSearch().find();
    }

    /**
     * Get application environment, checks for DEPLOYMENT_MANAGEMENT rights on it.
     *
     * @param environmentId
     * @return the environment if the current user has the proper rights on it
     * @throws java.nio.file.AccessDeniedException if the current user doesn't have proper rights on the requested environment
     */
    private ApplicationEnvironment checkAndGetApplicationEnvironment(String environmentId) {
        ApplicationEnvironment environment = environmentService.getOrFail(environmentId);
        Application application = applicationService.getOrFail(environment.getApplicationId());
        // Only a user with deployment rôle on the environment can create an associated service.
        AuthorizationUtil.checkAuthorizationForEnvironment(application, environment);
        return environment;
    }

    private ServiceResource getOrFail(String environmentId) {
        ServiceResource serviceResource = get(environmentId);
        if (serviceResource == null) {
            throw new NotFoundException("Service linked to the environement [" + environmentId + "] not found.");
        }

        return serviceResource;
    }

    /**
     * Unbind the service resource from the application environment
     *
     * Note that the service will still exists, but will only be updatable via service api
     * 
     * @param environmentId The environment for which to get the service resource.
     */
    public void unbind(String environmentId) {
        ServiceResource serviceResource = getOrFail(environmentId);
        serviceResource.setEnvironmentId(null);
        serviceResourceService.save(serviceResource);
        publisher.publishEvent(new ManagedServiceUnbindEvent(this, serviceResource));
    }

    /**
     * Delete the managed service resource
     *
     * Note that the service will still exists, but will only be updatable via service api
     *
     * @param environmentId The environment for which to get the service resource.
     */
    public void delete(String environmentId) {
        ServiceResource serviceResource = getOrFail(environmentId);
        serviceResourceService.delete(serviceResource.getId());
        publisher.publishEvent(new ManagedServiceDeletedEvent(this, serviceResource));
    }

    @EventListener
    private void onSubstitutionTypeChanged(SubstitutionTypeChangedEvent event) {
        ServiceResource[] serviceResources = serviceResourceService.getByNodeTypes(event.getSubstituteNodeType().getElementId(),
                event.getSubstituteNodeType().getArchiveVersion());
        // we just change non-running services as we don't actually update the service
        for (ServiceResource serviceResource : serviceResources) {
            if (ToscaNodeLifecycleConstants.INITIAL.equals(serviceResource.getState())) {
                log.debug("Trying to update node based on substitution type.");
                nodeInstanceService.update(event.getSubstituteNodeType(), serviceResource.getNodeInstance(),
                        serviceResource.getNodeInstance().getNodeTemplate().getProperties(),
                        serviceResource.getNodeInstance().getNodeTemplate().getCapabilities(), serviceResource.getNodeInstance().getAttributeValues());
                // Update relationships
                updateServiceRelationship(serviceResource, event.getTopology());
                serviceResourceService.save(serviceResource);
            } else {
                log.info("Substitution type <" + event.getSubstituteNodeType().getElementId() + ":" + event.getSubstituteNodeType().getArchiveVersion()
                        + "> for service <" + serviceResource.getName()
                        + "> has changed. Service will be updated on restart. Note that services based on snapshot versions are not recommended.");
            }
        }
    }

    private void updateServiceRelationship(String serviceId, Topology topology) {
        ServiceResource serviceResource = serviceResourceService.getOrFail(serviceId);
        updateServiceRelationship(serviceResource, topology);
    }

    private void updateServiceRelationship(ServiceResource serviceResource, Topology topology) {
        Map<String, RelationshipType> relationshipTypeMap = Maps.newHashMap();
        // we also want to configure the service relationships for exposed capabilities
        for (Entry<String, SubstitutionTarget> substitutionTargetEntry : safe(topology.getSubstitutionMapping().getCapabilities()).entrySet()) {
            if (serviceResource.getCapabilitiesRelationshipTypes() == null) {
                serviceResource.setCapabilitiesRelationshipTypes(Maps.newHashMap());
            }
            if (substitutionTargetEntry.getValue().getServiceRelationshipType() == null) {
                serviceResource.getCapabilitiesRelationshipTypes().remove(substitutionTargetEntry.getKey());
            } else {
                String relationshipId = getRelationshipId(relationshipTypeMap, topology, substitutionTargetEntry.getValue().getServiceRelationshipType());
                serviceResource.getCapabilitiesRelationshipTypes().put(substitutionTargetEntry.getKey(), relationshipId);
            }
        }
        for (Entry<String, SubstitutionTarget> substitutionTargetEntry : safe(topology.getSubstitutionMapping().getRequirements()).entrySet()) {
            if (serviceResource.getRequirementsRelationshipTypes() == null) {
                serviceResource.setRequirementsRelationshipTypes(Maps.newHashMap());
            }
            if (substitutionTargetEntry.getValue().getServiceRelationshipType() == null) {
                serviceResource.getRequirementsRelationshipTypes().remove(substitutionTargetEntry.getKey());
            } else {
                String relationshipId = getRelationshipId(relationshipTypeMap, topology, substitutionTargetEntry.getValue().getServiceRelationshipType());
                serviceResource.getRequirementsRelationshipTypes().put(substitutionTargetEntry.getKey(), relationshipId);
            }
        }
        serviceResourceService.save(serviceResource);
    }

    private String getRelationshipId(Map<String, RelationshipType> relationshipTypeMap, Topology topology, String relationshipTypeName) {
        RelationshipType relationshipType = relationshipTypeMap.computeIfAbsent(relationshipTypeName,
                k -> toscaTypeSearchService.getElementInDependencies(RelationshipType.class, relationshipTypeName, topology.getDependencies()));
        return relationshipType.getId();
    }
}
