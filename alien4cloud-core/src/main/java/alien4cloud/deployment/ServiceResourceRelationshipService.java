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

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.index.ToscaTypeSearchService;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.templates.ServiceNodeTemplate;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;

import alien4cloud.model.service.ServiceResource;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.ToscaRelationshipLifecycleConstants;
import alien4cloud.service.ServiceResourceService;

/**
 * Process the deployment topology to override service side of relationships (when node are matched againts services).
 */
@Component
public class ServiceResourceRelationshipService {
    @Inject
    private ServiceResourceService serviceResourceService;
    @Inject
    private ToscaTypeSearchService toscaTypeSearchService;

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
        // for services that are source of a relationship, all operations related to source (the service) are not run.
        String relationshipTypeId = serviceResource.getRequirementsRelationshipTypes().get(paaSRelationshipTemplate.getTemplate().getRequirementName());
        if (relationshipTypeId != null) {
            // The relationship may not exist in the topology archive so we don't use the TOSCA context but make a direct query
            RelationshipType relationshipType = toscaTypeSearchService.findByIdOrFail(RelationshipType.class, relationshipTypeId);
            Interface serviceInterface = safe(relationshipType.getInterfaces()).get(ToscaRelationshipLifecycleConstants.CONFIGURE);
            if (serviceInterface != null) {
                overrideOperations(templateInterface, serviceInterface, PRE_CONFIGURE_SOURCE, POST_CONFIGURE_SOURCE, ADD_TARGET, REMOVE_TARGET);
                return;
            }
        }
        // if operations are not overriden then just drop them.
        dropOperations(templateInterface, PRE_CONFIGURE_SOURCE, POST_CONFIGURE_SOURCE, ADD_TARGET, REMOVE_TARGET);
    }

    private void processTargetOperations(PaaSRelationshipTemplate paaSRelationshipTemplate, ServiceResource serviceResource, Interface templateInterface) {
        // for services that are target of a relationship, all operations related to target (the service) are not run.
        if (paaSRelationshipTemplate.getTemplate().getTargetedCapabilityName() != null) {
            String relationshipTypeId = serviceResource.getCapabilitiesRelationshipTypes()
                    .get(paaSRelationshipTemplate.getTemplate().getTargetedCapabilityName());
            if (relationshipTypeId != null) {
                RelationshipType relationshipType = toscaTypeSearchService.findByIdOrFail(RelationshipType.class, relationshipTypeId);
                Interface serviceInterface = safe(relationshipType.getInterfaces()).get(ToscaRelationshipLifecycleConstants.CONFIGURE);
                if (serviceInterface != null) {
                    overrideOperations(templateInterface, serviceInterface, PRE_CONFIGURE_TARGET, POST_CONFIGURE_TARGET, ADD_SOURCE, REMOVE_SOURCE);
                }
            }
        }
        dropOperations(templateInterface, PRE_CONFIGURE_TARGET, POST_CONFIGURE_TARGET, ADD_SOURCE, REMOVE_SOURCE);
    }

    /**
     * This method injects the operations of the configure interface from the service to override the ones defined on the template interface.
     * 
     * @param templateInterface The template interface on which to override source operations.
     * @param serviceInterface The service provided interface that will override source operations.
     * @param operations The operations to override.
     */
    private void overrideOperations(Interface templateInterface, Interface serviceInterface, String... operations) {
        for (String operation : operations) {
            templateInterface.getOperations().put(operation, serviceInterface.getOperations().get(operation));
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