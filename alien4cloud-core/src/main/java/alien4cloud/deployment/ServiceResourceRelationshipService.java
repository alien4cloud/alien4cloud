package alien4cloud.deployment;

import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.ADD_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.ADD_TARGET;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.POST_CONFIGURE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.POST_CONFIGURE_TARGET;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.PRE_CONFIGURE_TARGET;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.REMOVE_SOURCE;
import static alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants.REMOVE_TARGET;
import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map.Entry;

import javax.inject.Inject;

import org.alien4cloud.alm.service.ServiceResourceService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;

/**
 * Process the deployment topology to override service side of relationships (when node are matched againts services).
 */
@Component
public class ServiceResourceRelationshipService {
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private IToscaTypeSearchService toscaTypeSearchService;

    public void process(PaaSTopologyDeploymentContext deploymentContext) {
        for (PaaSNodeTemplate paaSNodeTemplate : deploymentContext.getPaaSTopology().getNonNatives()) {
            if (paaSNodeTemplate.getTemplate() instanceof ServiceNodeTemplate) {
                processService(paaSNodeTemplate);
            }
        }
    }

    private void processService(PaaSNodeTemplate paaSNodeTemplate) {
        ServiceNodeTemplate serviceNodeTemplate = (ServiceNodeTemplate) paaSNodeTemplate.getTemplate();
        ServiceResource serviceResource = serviceResourceService.getOrFail(serviceNodeTemplate.getServiceResourceId());
        for (PaaSRelationshipTemplate paaSRelationshipTemplate : paaSNodeTemplate.getRelationshipTemplates()) {
            Interface templateInterface = safe(paaSRelationshipTemplate.getInterfaces()).get(ToscaRelationshipLifecycleConstants.CONFIGURE);
            if (templateInterface != null) {
                if (paaSRelationshipTemplate.getSource().equals(paaSNodeTemplate.getId())) {
                    processSourceOperations(paaSRelationshipTemplate, serviceResource, templateInterface);
                } else {
                    processTargetOperations(paaSRelationshipTemplate, serviceResource, templateInterface);
                }
            }
        }
    }

    private void processSourceOperations(PaaSRelationshipTemplate paaSRelationshipTemplate, ServiceResource serviceResource, Interface templateInterface) {
        // Drop default source operations as the service is the source of a relationship
        dropOperations(templateInterface, PRE_CONFIGURE_SOURCE, POST_CONFIGURE_SOURCE, ADD_TARGET, REMOVE_TARGET);

        // for services that are source of a relationship, all operations related to source (the service) are not run.
        String relationshipTypeId = serviceResource.getRequirementsRelationshipTypes().get(paaSRelationshipTemplate.getTemplate().getRequirementName());
        if (relationshipTypeId != null) {
            // The relationship may not exist in the topology archive so we don't use the TOSCA context but make a direct query
            RelationshipType relationshipType = toscaTypeSearchService.findByIdOrFail(RelationshipType.class, relationshipTypeId);
            Interface serviceInterface = safe(relationshipType.getInterfaces()).get(ToscaRelationshipLifecycleConstants.CONFIGURE);
            if (serviceInterface != null) {
                overrideOperations(paaSRelationshipTemplate.getTemplate(), templateInterface, relationshipType, serviceInterface, PRE_CONFIGURE_SOURCE,
                        POST_CONFIGURE_SOURCE, ADD_TARGET, REMOVE_TARGET);
            }
        }
    }

    private void processTargetOperations(PaaSRelationshipTemplate paaSRelationshipTemplate, ServiceResource serviceResource, Interface templateInterface) {
        // Drop default target operations as the service is the target of a relationship
        dropOperations(templateInterface, PRE_CONFIGURE_TARGET, POST_CONFIGURE_TARGET, ADD_SOURCE, REMOVE_SOURCE);

        // for services that are target of a relationship, all operations related to target (the service) are not run.
        if (paaSRelationshipTemplate.getTemplate().getTargetedCapabilityName() != null && serviceResource.getCapabilitiesRelationshipTypes() != null) {
            String relationshipTypeId = serviceResource.getCapabilitiesRelationshipTypes()
                    .get(paaSRelationshipTemplate.getTemplate().getTargetedCapabilityName());
            if (relationshipTypeId != null) {
                RelationshipType relationshipType = toscaTypeSearchService.findByIdOrFail(RelationshipType.class, relationshipTypeId);
                Interface serviceInterface = safe(relationshipType.getInterfaces()).get(ToscaRelationshipLifecycleConstants.CONFIGURE);
                if (serviceInterface != null) {
                    overrideOperations(paaSRelationshipTemplate.getTemplate(), templateInterface, relationshipType, serviceInterface, PRE_CONFIGURE_TARGET,
                            POST_CONFIGURE_TARGET, ADD_SOURCE, REMOVE_SOURCE);
                }
            }
        }
    }

    /**
     * This method injects the operations of the configure interface from the service to override the ones defined on the template interface.
     *
     * @param relationshipTemplate The relationships template that contains the template interface. Artifacts from the service relationship are going to be
     *            injected.
     * @param templateInterface The template interface on which to override source operations.
     * @param serviceRelationshipType The relationship type associated with the service capability or requirement.
     * @param serviceInterface The service provided interface that will override source operations.
     * @param operations The operations to override.
     */
    private void overrideOperations(RelationshipTemplate relationshipTemplate, Interface templateInterface, RelationshipType serviceRelationshipType,
            Interface serviceInterface, String... operations) {
        for (String operation : operations) {
            templateInterface.getOperations().put(operation, serviceInterface.getOperations().get(operation));
        }
        // We also need to inject relationships artifacts from the service.
        if (relationshipTemplate.getProperties() == null) {
            relationshipTemplate.setProperties(Maps.newHashMap());
        }
        for (Entry<String, PropertyDefinition> propertyEntry : safe(serviceRelationshipType.getProperties()).entrySet()) {
            if (propertyEntry.getValue().getDefault() != null) {
                if (relationshipTemplate.getProperties().containsKey(propertyEntry.getKey())) {
                    throw new IllegalArgumentException(
                            "The service relationship requires to override a property already used/defined by the template relationship. This association cannot be done.");
                }
                relationshipTemplate.getProperties().put(propertyEntry.getKey(), propertyEntry.getValue().getDefault());
            }
        }

        // and relationships artifacts from the service.
        if (relationshipTemplate.getArtifacts() == null) {
            relationshipTemplate.setArtifacts(Maps.newHashMap());
        }
        for (Entry<String, DeploymentArtifact> artifactEntry : safe(serviceRelationshipType.getArtifacts()).entrySet()) {
            if (relationshipTemplate.getArtifacts().containsKey(artifactEntry.getKey())) {
                throw new IllegalArgumentException(
                        "The service relationship requires to override an artifact already used/defined by the template relationship. This association cannot be done.");
            }
            relationshipTemplate.getArtifacts().put(artifactEntry.getKey(), artifactEntry.getValue());
        }
    }

    /***
     * This method drop all the operations of the configure interface that are supposed to be executed on the source node.
     * 
     * @param templateInterface The template interface from which to drop source operations.
     * @param operations The operations to override.
     */
    private void dropOperations(Interface templateInterface, String... operations) {
        for (String operation : operations) {
            templateInterface.getOperations().remove(operation);
        }
    }
}