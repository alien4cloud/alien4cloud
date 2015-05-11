package alien4cloud.rest.topology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.Valid;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import alien4cloud.application.ApplicationEnvironmentService;
import alien4cloud.application.ApplicationVersionService;
import alien4cloud.application.DeploymentSetupService;
import alien4cloud.cloud.CloudService;
import alien4cloud.component.CSARRepositorySearchService;
import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.component.repository.IFileRepository;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.ApplicationEnvironment;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.application.DeploymentSetup;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedCapabilityType;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.ScalarPropertyValue;
import alien4cloud.model.topology.AbstractPolicy;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.HaPolicy;
import alien4cloud.model.topology.NodeGroup;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.ScalingPolicy;
import alien4cloud.model.topology.Topology;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.plan.BuildPlanGenerator;
import alien4cloud.paas.plan.StartEvent;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.rest.model.RestErrorBuilder;
import alien4cloud.rest.model.RestErrorCode;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.model.RestResponseBuilder;
import alien4cloud.security.ApplicationRole;
import alien4cloud.topology.TopologyDTO;
import alien4cloud.topology.TopologyService;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.topology.TopologyValidationResult;
import alien4cloud.topology.TopologyValidationService;
import alien4cloud.tosca.properties.constraints.ConstraintUtil.ConstraintInformation;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.Authorization;

@Slf4j
@RestController
@RequestMapping("/rest/topologies")
public class TopologyController {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Resource
    private CSARRepositorySearchService csarRepoSearch;

    @Resource
    private ConstraintPropertyService constraintPropertyService;

    @Resource
    private TopologyService topologyService;

    @Resource
    private TopologyValidationService topologyValidationService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;

    @Resource
    private IFileRepository artifactRepository;

    @Resource
    private ApplicationEnvironmentService applicationEnvironmentService;

    @Resource
    private ApplicationVersionService applicationVersionService;

    @Resource
    private DeploymentSetupService deploymentSetupService;

    @Resource
    private CloudService cloudService;

    /**
     * Retrieve an existing {@link alien4cloud.model.topology.Topology}
     *
     * @param topologyId The id of the topology to retrieve.
     * @return {@link RestResponse}<{@link TopologyDTO}> containing the {@link alien4cloud.model.topology.Topology} and the {@link IndexedNodeType} related
     *         to his {@link alien4cloud.model.topology.NodeTemplate}s
     */
    @ApiOperation(value = "Retrieve a topology from it's id.", notes = "Returns a topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> get(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService
                .checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_USER);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @RequestMapping(value = "/{topologyId}/yaml", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<String> getYaml(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService
                .checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_USER);

        String yaml = topologyService.getYaml(topology);
        return RestResponseBuilder.<String> builder().data(yaml).build();
    }

    /**
     * Add a node template to a topology based on a node type
     *
     * @param topologyId
     *            The id of the topology for which to add the node template.
     * @param nodeTemplateRequest
     *            The request that contains the name and type of the node template to add.
     * @return TopologyDTO The DTO of the modified topology.
     */
    @ApiOperation(value = "Add a new node template in a topology.", notes = "Returns the details of the node template (computed from it's type). Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addNodeTemplate(@PathVariable String topologyId, @RequestBody @Valid NodeTemplateRequest nodeTemplateRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, nodeTemplateRequest.getIndexedNodeTypeId());
        if (indexedNodeType == null) {
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR).build()).build();
        }

        if (topology.getNodeTemplates() == null) {
            topology.setNodeTemplates(new HashMap<String, NodeTemplate>());
        }

        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Set<String> nodeTemplatesNames = nodeTemplates.keySet();
        if (nodeTemplatesNames.contains(nodeTemplateRequest.getName())) {
            log.debug("Add Node Template <{}> impossible (already exists)", nodeTemplateRequest.getName());
            // a node template already exist with the given name.
            throw new AlreadyExistException("A node template with the given name already exists.");
        } else if (!NodeTemplate.isValidNodeTemplateName(nodeTemplateRequest.getName())) {
            return RestResponseBuilder
                    .<TopologyDTO> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message("The name can only contain characters a to Z, dashes and without spaces.").build()).build();
        } else {
            log.debug("Create application <{}>", nodeTemplateRequest.getName());
        }
        indexedNodeType = topologyService.loadType(topology, indexedNodeType);
        NodeTemplate nodeTemplate = topologyService.buildNodeTemplate(topology.getDependencies(), indexedNodeType, null);
        topology.getNodeTemplates().put(nodeTemplateRequest.getName(), nodeTemplate);

        log.debug("Adding a new Node template <" + nodeTemplateRequest.getName() + "> bound to the node type <" + nodeTemplateRequest.getIndexedNodeTypeId()
                + "> to the topology <" + topology.getId() + "> .");

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Add a new scaling policy for a node template in a topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/scalingPolicies/{nodeTemplateId}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addScalingPolicy(@PathVariable String topologyId, @PathVariable String nodeTemplateId,
            @RequestBody @Valid ScalingPolicy policy) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, ScalingPolicy> policies = topology.getScalingPolicies();
        if (policies == null) {
            policies = Maps.newHashMap();
            topology.setScalingPolicies(policies);
        }
        policies.put(nodeTemplateId, policy);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove scaling policy from a compute in a topology.", notes = "Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/scalingPolicies/{nodeTemplateId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> deleteScalingPolicy(@PathVariable String topologyId, @PathVariable String nodeTemplateId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, ScalingPolicy> policies = topology.getScalingPolicies();
        if (policies == null) {
            throw new NotFoundException("Scaling policy not found for node [" + nodeTemplateId + "] of topology [" + topologyId + "]");
        } else {
            if (policies.remove(nodeTemplateId) == null) {
                throw new NotFoundException("Scaling policy not found for node [" + nodeTemplateId + "]");
            }
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Update the name of a node template.
     *
     * @param topologyId The id of the topology in which the node template to update lies.
     * @param nodeTemplateName The name of the node template to update.
     * @param newNodeTemplateName The new name for the node template.
     * @return {@link RestResponse}<{@link Void}> an response with no data and no error if successful.
     */
    @ApiOperation(value = "Change the name of a node template in a topology.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/updateName/{newNodeTemplateName}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> updateNodeTemplateName(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String newNodeTemplateName) {
        if (!NodeTemplate.isValidNodeTemplateName(newNodeTemplateName)) {
            return RestResponseBuilder
                    .<TopologyDTO> builder()
                    .error(RestErrorBuilder.builder(RestErrorCode.INTERNAL_OBJECT_ERROR)
                            .message("The name can only contain characters a to Z, dashes and without spaces.").build()).build();
        }

        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        isUniqueNodeTemplateName(topologyId, newNodeTemplateName, nodeTemplates);

        nodeTemplates.put(newNodeTemplateName, nodeTemplate);
        nodeTemplates.remove(nodeTemplateName);
        refreshNodeTempNameInRelationships(nodeTemplateName, newNodeTemplateName, nodeTemplates);
        updateArtifactsOnNodeTemplateNameChange(nodeTemplateName, newNodeTemplateName, topology);
        updateGroupMembers(topology, nodeTemplate, nodeTemplateName, newNodeTemplateName);

        log.debug("Renaming the Node template <{}> with <{}> in the topology <{}> .", nodeTemplateName, newNodeTemplateName, topologyId);

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Update properties and artifacts inputs in a topology
     */
    private void updateArtifactsOnNodeTemplateNameChange(String oldNodeTemplateName, String newNodeTemplateName, Topology topology) {
        // Input artifacts
        if (topology.getInputArtifacts() != null) {
            Set<String> oldArtifactsInputs = topology.getInputArtifacts().remove(oldNodeTemplateName);
            if (oldArtifactsInputs != null) {
                topology.getInputArtifacts().put(newNodeTemplateName, oldArtifactsInputs);
            }
        }

        // Output properties
        if (topology.getOutputProperties() != null) {
            Set<String> oldPropertiesOutputs = topology.getOutputProperties().remove(oldNodeTemplateName);
            if (oldPropertiesOutputs != null) {
                topology.getOutputProperties().put(newNodeTemplateName, oldPropertiesOutputs);
            }
        }

    }

    /**
     * <p>
     * Update the name of a node template in the relationships of a topology. This requires two operations:
     * <ul>
     * <li>Rename the target node of a relationship</li>
     * <li>If a relationship has an auto-generated id, update it's id to take in account the new target name.</li>
     * </ul>
     * </p>
     *
     * @param oldNodeTemplateName Name of the node template that changes.
     * @param newNodeTemplateName New name for the node template.
     * @param nodeTemplates Map of all node templates in the topology.
     */
    private void refreshNodeTempNameInRelationships(String oldNodeTemplateName, String newNodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        // node templates copy
        for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
            if (nodeTemplate.getRelationships() != null) {
                refreshNodeTemplateNameInRelationships(oldNodeTemplateName, newNodeTemplateName, nodeTemplate.getRelationships());
            }
        }
    }

    private void refreshNodeTemplateNameInRelationships(String oldNodeTemplateName, String newNodeTemplateName,
            Map<String, RelationshipTemplate> relationshipTemplates) {
        Map<String, String> updatedKeys = Maps.newHashMap();
        for (Entry<String, RelationshipTemplate> relationshipTemplateEntry : relationshipTemplates.entrySet()) {
            String relationshipTemplateId = relationshipTemplateEntry.getKey();
            RelationshipTemplate relationshipTemplate = relationshipTemplateEntry.getValue();

            if (relationshipTemplate.getTarget().equals(oldNodeTemplateName)) {
                relationshipTemplate.setTarget(newNodeTemplateName);
                String formatedOldNodeName = topologyService.getRelationShipName(relationshipTemplate.getType(), oldNodeTemplateName);
                // if the id/name of the relationship is auto-generated we should update it also as auto-generation is <typeName+targetId>
                if (relationshipTemplateId.equals(formatedOldNodeName)) {
                    String newRelationshipTemplateId = topologyService.getRelationShipName(relationshipTemplate.getType(), newNodeTemplateName);
                    // check that the new name is not already used (so we won't override another relationship)...
                    String validNewRelationshipTemplateId = newRelationshipTemplateId;
                    int counter = 0;
                    while (relationshipTemplates.containsKey(validNewRelationshipTemplateId)) {
                        validNewRelationshipTemplateId = newRelationshipTemplateId + counter;
                        counter++;
                    }
                    updatedKeys.put(relationshipTemplateId, validNewRelationshipTemplateId);
                }
            }
        }

        // update the relationship keys if any has been impacted
        for (Entry<String, String> updateKeyEntry : updatedKeys.entrySet()) {
            RelationshipTemplate relationshipTemplate = relationshipTemplates.remove(updateKeyEntry.getKey());
            relationshipTemplates.put(updateKeyEntry.getValue(), relationshipTemplate);
        }
    }

    private void isUniqueNodeTemplateName(String topologyId, String newNodeTemplateName, Map<String, NodeTemplate> nodeTemplates) {
        if (nodeTemplates.containsKey(newNodeTemplateName)) {
            log.debug("Add Node Template <{}> impossible (already exists)", newNodeTemplateName);
            // a node template already exist with the given name.
            throw new AlreadyExistException("A node template with the given name " + newNodeTemplateName + " already exists in the topology " + topologyId
                    + ".");
        }
    }

    private void isUniqueRelationshipName(String topologyId, String nodeTemplateName, String newName, Set<String> relationshipNames) {
        if (relationshipNames.contains(newName)) {
            // a relation already exist with the given name.
            throw new AlreadyExistException("A relationship with the given name " + newName + " already exists in the node template " + nodeTemplateName
                    + " of topology " + topologyId + ".");
        }
    }

    /**
     * Add a new {@link RelationshipTemplate} to a {@link NodeTemplate} in a {@link Topology}.
     *
     * @param topologyId The id of the topology in which the node template lies.
     * @param nodeTemplateName The name of the node template to which we should add the relationship.
     * @param relationshipName The name of the relationship to add.
     * @param relationshipTemplateRequest The relationship.
     * @return A rest response with no errors if successful.
     */
    @ApiOperation(value = "Add a relationship to a node template.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId}/nodetemplates/{nodeTemplateName}/relationships/{relationshipName}", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addRelationshipTemplate(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String relationshipName, @RequestBody AddRelationshipTemplateRequest relationshipTemplateRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        IndexedRelationshipType indexedRelationshipType = alienDAO.findById(IndexedRelationshipType.class, relationshipTemplateRequest
                .getRelationshipTemplate().getType() + ":" + relationshipTemplateRequest.getArchiveVersion());
        if (indexedRelationshipType == null) {
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.COMPONENT_MISSING_ERROR).build()).build();
        }
        topologyService.loadType(topology, indexedRelationshipType);
        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);

        boolean upperBoundReachedSource = topologyValidationService.isRequirementUpperBoundReachedForSource(nodeTemplate, relationshipTemplateRequest
                .getRelationshipTemplate().getRequirementName(), topology.getDependencies());
        // return with a rest response error
        if (upperBoundReachedSource) {
            return RestResponseBuilder
                    .<TopologyDTO> builder()
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.UPPER_BOUND_REACHED)
                            .message(
                                    "UpperBound reached on requirement <" + relationshipTemplateRequest.getRelationshipTemplate().getRequirementName()
                                            + "> on node <" + nodeTemplateName + ">.").build()).build();
        }

        boolean upperBoundReachedTarget = topologyValidationService.isCapabilityUpperBoundReachedForTarget(relationshipTemplateRequest
                .getRelationshipTemplate().getTarget(), nodeTemplates, relationshipTemplateRequest.getRelationshipTemplate().getTargetedCapabilityName(),
                topology.getDependencies());
        // return with a rest response error
        if (upperBoundReachedTarget) {
            return RestResponseBuilder
                    .<TopologyDTO> builder()
                    .error(RestErrorBuilder
                            .builder(RestErrorCode.UPPER_BOUND_REACHED)
                            .message(
                                    "UpperBound reached on capability <" + relationshipTemplateRequest.getRelationshipTemplate().getTargetedCapabilityName()
                                            + "> on node <" + relationshipTemplateRequest.getRelationshipTemplate().getTarget() + ">.").build()).build();
        }

        Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
        if (relationships == null) {
            relationships = Maps.newHashMap();
            nodeTemplates.get(nodeTemplateName).setRelationships(relationships);
        }

        RelationshipTemplate relationship = relationshipTemplateRequest.getRelationshipTemplate();
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        TopologyServiceCore.fillProperties(properties, indexedRelationshipType.getProperties(), null);
        relationship.setProperties(properties);
        relationships.put(relationshipName, relationship);
        alienDAO.save(topology);
        log.info("Added relationship to the topology [" + topologyId + "], node name [" + nodeTemplateName + "], relationship name [" + relationshipName + "]");
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Remove a nodeTemplate outputs in a topology
     */
    private void removeOutputs(String nodeTemplateName, Topology topology) {
        if (topology.getOutputProperties() != null) {
            topology.getOutputProperties().remove(nodeTemplateName);
        }
        if (topology.getOutputAttributes() != null) {
            topology.getOutputAttributes().remove(nodeTemplateName);
        }
    }

    /**
     * Delete a node template from a topology
     *
     * @param topologyId Id of the topology from which to delete the node template.
     * @param nodeTemplateName Id of the node template to delete.
     * @return NodeTemplateDTO The DTO containing the newly deleted node template and the related node type
     */
    @ApiOperation(value = "Delete a node tempalte from a topology", notes = "If successful returns a result containing the list of impacted nodes (that will loose relationships). Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> deleteNodeTemplate(@PathVariable String topologyId, @PathVariable String nodeTemplateName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        log.debug("Removing the Node template <{}> from the topology <{}> .", nodeTemplateName, topology.getId());

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);

        NodeTemplate template = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        // Clean up internal repository
        Map<String, DeploymentArtifact> artifacts = template.getArtifacts();
        if (artifacts != null) {
            for (Map.Entry<String, DeploymentArtifact> artifactEntry : artifacts.entrySet()) {
                DeploymentArtifact artifact = artifactEntry.getValue();
                if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
                    this.artifactRepository.deleteFile(artifact.getArtifactRef());
                }
            }
        }
        List<String> typesTobeUnloaded = Lists.newArrayList();
        // Clean up dependencies of the topology
        typesTobeUnloaded.add(template.getType());
        if (template.getRelationships() != null) {
            for (RelationshipTemplate relationshipTemplate : template.getRelationships().values()) {
                typesTobeUnloaded.add(relationshipTemplate.getType());
            }
        }
        topologyService.unloadType(topology, typesTobeUnloaded.toArray(new String[typesTobeUnloaded.size()]));
        removeRelationShipReferences(nodeTemplateName, topology);
        nodeTemplates.remove(nodeTemplateName);
        removeArtifactsAndPolicies(nodeTemplateName, topology);
        removeOutputs(nodeTemplateName, topology);

        // group members removal
        updateGroupMembers(topology, template, nodeTemplateName, null);

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Manage node group members when a node name is removed or its name has changed.
     * 
     * @param newName : the new name of the node or <code>null</code> if the node has been removed.
     */
    private void updateGroupMembers(Topology topology, NodeTemplate template, String nodeName, String newName) {
        Map<String, NodeGroup> topologyGroups = topology.getGroups();
        if (template.getGroups() != null && !template.getGroups().isEmpty() && topologyGroups != null) {
            for (String groupId : template.getGroups()) {
                NodeGroup nodeGroup = topologyGroups.get(groupId);
                if (nodeGroup != null && nodeGroup.getMembers() != null) {
                    boolean removed = nodeGroup.getMembers().remove(nodeName);
                    if (removed && newName != null) {
                        nodeGroup.getMembers().add(newName);
                    }
                }
            }
        }
    }

    private void removeArtifactsAndPolicies(String nodeTemplateName, Topology topology) {
        if (topology.getScalingPolicies() != null) {
            topology.getScalingPolicies().remove(nodeTemplateName);
        }
        if (topology.getInputArtifacts() != null) {
            topology.getInputArtifacts().remove(nodeTemplateName);
        }
    }

    /**
     * Build and return a RestResponse if we detected a property constraint violation
     * 
     * @param propertyName property's name
     * @param propertyValue property's value
     * @param propertyDefinition property's definition
     * @return response containing validation result
     */
    private RestResponse<ConstraintInformation> buildRestErrorIfPropertyConstraintViolation(final String propertyName, final String propertyValue,
            final PropertyDefinition propertyDefinition) {
        try {
            constraintPropertyService.checkPropertyConstraint(propertyName, propertyValue, propertyDefinition);
        } catch (ConstraintViolationException e) {
            log.error("Constraint violation error for property <" + propertyName + "> with value <" + propertyValue + ">", e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_CONSTRAINT_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
            log.error("Constraint value violation error for property <" + e.getConstraintInformation().getName() + "> with value <"
                    + e.getConstraintInformation().getValue() + "> and type <" + e.getConstraintInformation().getType() + ">", e);
            return RestResponseBuilder.<ConstraintInformation> builder().data(e.getConstraintInformation())
                    .error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_TYPE_VIOLATION_ERROR).message(e.getMessage()).build()).build();
        }
        return null;
    }

    /**
     * Update one property for a given {@link NodeTemplate}
     *
     * @param topologyId The id of the topology that contains the node template for which to update a property.
     * @param nodeTemplateName The name of the node template for which to update a property.
     * @param updatePropertyRequest The key and value of the property to update.
     * @return a void rest response that contains no data if successful and an error if something goes wrong.
     */
    @ApiOperation(value = "Update properties values.", notes = "Returns a topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/properties", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ConstraintInformation> updatePropertyValue(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @RequestBody UpdatePropertyRequest updatePropertyRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemp = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        String propertyName = updatePropertyRequest.getPropertyName();
        String propertyValue = updatePropertyRequest.getPropertyValue();

        IndexedNodeType node = csarRepoSearch.getElementInDependencies(IndexedNodeType.class, nodeTemp.getType(), topology.getDependencies());

        if (!node.getProperties().containsKey(propertyName)) {
            throw new NotFoundException("Property <" + propertyName + "> doesn't exists for node <" + nodeTemplateName + "> of type <" + nodeTemp.getType()
                    + ">");
        }

        RestResponse<ConstraintInformation> response = buildRestErrorIfPropertyConstraintViolation(propertyName, propertyValue,
                node.getProperties().get(propertyName));
        if (response != null) {
            return response;
        }

        log.debug("Updating property <{}> of the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].", propertyName, nodeTemplateName,
                topology.getId(), nodeTemp.getProperties().get(propertyName), propertyValue);

        nodeTemp.getProperties().put(propertyName, new ScalarPropertyValue(propertyValue));
        alienDAO.save(topology);

        return RestResponseBuilder.<ConstraintInformation> builder().build();
    }

    /**
     * Update one property for a given @{IndexedRelationshipType} of a {@link NodeTemplate}
     *
     * @param topologyId The id of the topology that contains the node template for which to update a property.
     * @param nodeTemplateName The name of the node template for which to update a property.
     * @param updatePropertyRequest The key and value of the property to update.
     * @return a void rest response that contains no data if successful and an error if something goes wrong.
     */
    @ApiOperation(value = "Update a relationship property value.", notes = "Returns a topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/relationships/{relationshipName}/updateProperty", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ConstraintInformation> updateRelationshipPropertyValue(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String relationshipName, @RequestBody UpdateIndexedTypePropertyRequest updatePropertyRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        String propertyName = updatePropertyRequest.getPropertyName();
        String propertyValue = updatePropertyRequest.getPropertyValue();
        String relationshipType = updatePropertyRequest.getType();
        Map<String, IndexedRelationshipType> relationshipTypes = topologyServiceCore.getIndexedRelationshipTypesFromTopology(topology);

        if (!relationshipTypes.get(relationshipType).getProperties().containsKey(propertyName)) {
            throw new NotFoundException("Property <" + propertyName + "> doesn't exists for node <" + nodeTemplateName + "> of type <" + relationshipType + ">");
        }

        RestResponse<ConstraintInformation> response = buildRestErrorIfPropertyConstraintViolation(propertyName, propertyValue,
                relationshipTypes.get(relationshipType).getProperties().get(propertyName));
        if (response != null) {
            return response;
        }

        log.debug("Updating property <{}> of the relationship <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                propertyName, relationshipType, nodeTemplateName, topology.getId(), relationshipTypes.get(relationshipType).getProperties().get(propertyName),
                propertyValue);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
        relationships.get(relationshipName).getProperties().put(propertyName, new ScalarPropertyValue(propertyValue));

        alienDAO.save(topology);
        return RestResponseBuilder.<ConstraintInformation> builder().build();
    }

    /**
     * Update one property for a given @{IndexedCapabilityType} of a {@link NodeTemplate}
     *
     * @param topologyId The id of the topology that contains the node template for which to update a property.
     * @param nodeTemplateName The name of the node template for which to update a property.
     * @param capabilityId The name of the capability.
     * @param updatePropertyRequest The key and value of the property to update.
     * @return a void rest response that contains no data if successful and an error if something goes wrong.
     */
    @ApiOperation(value = "Update a relationship property value.", notes = "Returns a topology with it's details. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/updateProperty", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<ConstraintInformation> updateCapabilityPropertyValue(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String capabilityId, @RequestBody UpdateIndexedTypePropertyRequest updatePropertyRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        String propertyName = updatePropertyRequest.getPropertyName();
        String propertyValue = updatePropertyRequest.getPropertyValue();
        String capabilityType = updatePropertyRequest.getType();
        Map<String, IndexedCapabilityType> capabilityTypes = topologyServiceCore.getIndexedCapabilityTypesFromTopology(topology);

        if (!capabilityTypes.get(capabilityType).getProperties().containsKey(propertyName)) {
            throw new NotFoundException("Property <" + propertyName + "> doesn't exists for node <" + nodeTemplateName + "> of type <" + capabilityType + ">");
        }

        RestResponse<ConstraintInformation> response = buildRestErrorIfPropertyConstraintViolation(propertyName, propertyValue,
                capabilityTypes.get(capabilityType).getProperties().get(propertyName));
        if (response != null) {
            return response;
        }

        log.debug("Updating property <{}> of the capability <{}> for the Node template <{}> from the topology <{}>: changing value from [{}] to [{}].",
                propertyName, capabilityType, nodeTemplateName, topology.getId(), capabilityTypes.get(capabilityType).getProperties().get(propertyName),
                propertyValue);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        Map<String, Capability> capabilities = nodeTemplate.getCapabilities();
        capabilities.get(capabilityId).getProperties().put(propertyName, new ScalarPropertyValue(propertyValue));

        alienDAO.save(topology);
        return RestResponseBuilder.<ConstraintInformation> builder().build();
    }

    /**
     * check if a topology is valid or not.
     *
     * @param topologyId The id of the topology to check.
     * @return a boolean rest response that says if the topology is valid or not.
     */
    @ApiOperation(value = "Check if a topology is valid or not.", notes = "Returns true if valid, false if not. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/isvalid", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyValidationResult> isTopologyValid(@PathVariable String topologyId, @RequestParam(required = false) String environmentId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService
                .checkAuthorizations(topology, ApplicationRole.APPLICATION_MANAGER, ApplicationRole.APPLICATION_DEVOPS, ApplicationRole.APPLICATION_USER);
        DeploymentSetup deploymentSetup = null;
        CloudResourceMatcherConfig cloudResourceMatcherConfig = null;
        if (StringUtils.isNotBlank(environmentId)) {
            ApplicationEnvironment environment = applicationEnvironmentService.getEnvironmentByIdOrDefault(null, environmentId);
            ApplicationVersion version = applicationVersionService.getVersionByIdOrDefault(environment.getApplicationId(), environment.getCurrentVersionId());
            deploymentSetup = deploymentSetupService.getDeploymentSetupMatchInfo(topology, environment, version);
            if (StringUtils.isNotEmpty(environment.getCloudId())) {
                cloudResourceMatcherConfig = cloudService.getCloudResourceMatcherConfig(cloudService.getMandatoryCloud(environment.getCloudId()));
            }
        }
        TopologyValidationResult dto = topologyValidationService.validateTopology(topology, deploymentSetup, cloudResourceMatcherConfig);
        return RestResponseBuilder.<TopologyValidationResult> builder().data(dto).build();
    }

    /**
     * Get possible replacement indexedNodeTypes for a node template
     *
     * @param topologyId The id of the topology to check.
     * @param nodeTemplateName The name of the node template to check for replacement.
     * @return An array of indexedNodeType which can replace the node template
     */
    @ApiOperation(value = "Get possible replacement indexedNodeTypes for a node template.", notes = "Returns An array of indexedNodeType which can replace the node template. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/replace", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<IndexedNodeType[]> getReplacementForNode(@PathVariable String topologyId, @PathVariable String nodeTemplateName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, topologyServiceCore.getNodeTemplates(topology));

        IndexedNodeType[] replacementsNodeTypes = topologyService.findReplacementForNode(nodeTemplateName, topology);

        return RestResponseBuilder.<IndexedNodeType[]> builder().data(replacementsNodeTypes).build();
    }

    /**
     * Replace a node template
     */
    @ApiOperation(value = "Replace a node template possible with another one.", notes = "Returns the details of the new node template (computed from it's type). Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/replace", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> replaceNodeTemplate(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @RequestBody @Valid NodeTemplateRequest nodeTemplateRequest) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        IndexedNodeType indexedNodeType = findIndexedNodeType(nodeTemplateRequest.getIndexedNodeTypeId());

        // Retrieve existing node template
        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate oldNodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        // Load the new type to the topology in order to update its dependencies
        indexedNodeType = topologyService.loadType(topology, indexedNodeType);
        // Build the new one
        NodeTemplate newNodeTemplate = topologyService.buildNodeTemplate(topology.getDependencies(), indexedNodeType, null);
        newNodeTemplate.setRelationships(oldNodeTemplate.getRelationships());
        // Put the new one in the topology
        nodeTemplates.put(nodeTemplateRequest.getName(), newNodeTemplate);

        // Unload and remove old node template
        topologyService.unloadType(topology, oldNodeTemplate.getType());
        nodeTemplates.remove(nodeTemplateName);
        removeArtifactsAndPolicies(nodeTemplateName, topology);

        refreshNodeTempNameInRelationships(nodeTemplateName, nodeTemplateRequest.getName(), nodeTemplates);
        log.debug("Replacing the node template<{}> with <{}> bound to the node type <{}> on the topology <{}> .", nodeTemplateName,
                nodeTemplateRequest.getName(), nodeTemplateRequest.getIndexedNodeTypeId(), topology.getId());
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Update application's artifact.
     *
     * @param topologyId The topology's id
     * @param nodeTemplateName The node template's name
     * @param artifactId artifact's id
     * @return nothing if success, error will be handled in global exception strategy
     * @throws IOException
     */
    @ApiOperation(value = "Updates the deployment artifact of the node template.", notes = "The logged-in user must have the application manager role for this application. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/artifacts/{artifactId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> updateDeploymentArtifact(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String artifactId, @RequestParam("file") MultipartFile artifactFile) throws IOException {
        // Perform check that authorization's ok
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        // Get the node template's artifacts to update
        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        Map<String, DeploymentArtifact> artifacts = nodeTemplate.getArtifacts();
        if (artifacts == null) {
            throw new NotFoundException("Artifact with key [" + artifactId + "] do not exist");
        }
        DeploymentArtifact artifact = artifacts.get(artifactId);
        if (artifact == null) {
            throw new NotFoundException("Artifact with key [" + artifactId + "] do not exist");
        }
        String oldArtifactId = artifact.getArtifactRef();
        if (ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY.equals(artifact.getArtifactRepository())) {
            artifactRepository.deleteFile(oldArtifactId);
        }
        InputStream artifactStream = artifactFile.getInputStream();
        try {
            String artifactFileId = artifactRepository.storeFile(artifactStream);
            artifact.setArtifactName(artifactFile.getOriginalFilename());
            artifact.setArtifactRef(artifactFileId);
            artifact.setArtifactRepository(ArtifactRepositoryConstants.ALIEN_ARTIFACT_REPOSITORY);
            alienDAO.save(topology);
            return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
        } finally {
            Closeables.close(artifactStream, true);
        }
    }

    private Map<String, NodeTemplate> removeRelationShipReferences(String nodeTemplateName, Topology topology) {
        Map<String, NodeTemplate> nodeTemplates = topology.getNodeTemplates();
        Map<String, NodeTemplate> impactedNodeTemplates = Maps.newHashMap();
        List<String> keysToRemove = Lists.newArrayList();
        for (String key : nodeTemplates.keySet()) {
            NodeTemplate nodeTemp = nodeTemplates.get(key);
            if (nodeTemp.getRelationships() == null) {
                continue;
            }
            keysToRemove.clear();
            for (String key2 : nodeTemp.getRelationships().keySet()) {
                RelationshipTemplate relTemp = nodeTemp.getRelationships().get(key2);
                if (relTemp == null) {
                    continue;
                }
                if (relTemp.getTarget() != null && relTemp.getTarget().equals(nodeTemplateName)) {
                    keysToRemove.add(key2);
                }
            }
            for (String relName : keysToRemove) {
                nodeTemplates.get(key).getRelationships().remove(relName);
                impactedNodeTemplates.put(key, nodeTemplates.get(key));
            }
        }
        return impactedNodeTemplates.isEmpty() ? null : impactedNodeTemplates;
    }

    private IndexedNodeType findIndexedNodeType(final String indexedNodeTypeId) {
        IndexedNodeType indexedNodeType = alienDAO.findById(IndexedNodeType.class, indexedNodeTypeId);
        if (indexedNodeType == null) {
            throw new NotFoundException("Indexed Node Type [" + indexedNodeTypeId + "] cannot be found");
        }
        return indexedNodeType;
    }

    /**
     * Delete a {@link RelationshipTemplate} from a {@link NodeTemplate} in a {@link Topology}.
     *
     * @param topologyId The id of the topology in which the node template lies.
     * @param nodeTemplateName The name of the node template from which we should delete the relationship.
     * @param relationshipName The name of the relationship to delete.
     * @return A rest response with no errors if successful.
     */
    @ApiOperation(value = "Delete a relationship from a node template.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/relationships/{relationshipName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> deleteRelationshipTemplate(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String relationshipName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);

        NodeTemplate template = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);
        log.debug("Removing the Relationship template <" + relationshipName + "> from the Node template <" + nodeTemplateName + ">, Topology <"
                + topology.getId() + "> .");
        RelationshipTemplate relationshipTemplate = template.getRelationships().get(relationshipName);
        if (relationshipTemplate != null) {
            topologyService.unloadType(topology, relationshipTemplate.getType());
            template.getRelationships().remove(relationshipName);
        } else {
            throw new NotFoundException("The relationship with name [" + relationshipName + "] do not exist for the node [" + nodeTemplateName
                    + "] of the topology [" + topologyId + "]");
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Activate a property as an output property.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/property/{propertyName}/isOutput", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addOutputProperty(@PathVariable String topologyId, @PathVariable String nodeTemplateName, @PathVariable String propertyName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);

        if (nodeTemplate.getProperties() != null && nodeTemplate.getProperties().containsKey(propertyName)) {
            topology.setOutputProperties(addToMap(topology.getOutputProperties(), nodeTemplateName, propertyName));
        } else {
            // attributeName does not exists in the node template
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_MISSING_ERROR).build()).build();
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /*
     * Get the output capability properties of an topology or throw an exception if an element is not found
     */
    private Map<String, Map<String, Set<String>>> getOutputCapabilityPropertiesOrThrowException(Topology topology, String nodeTemplateName, String propertyId,
            String capabilityId) {
        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topology.getId(), nodeTemplateName, nodeTemplates);

        if (nodeTemplate.getCapabilities() == null || nodeTemplate.getCapabilities().get(capabilityId) == null) {
            throw new NotFoundException("Capability " + capabilityId + " do not exist for the node " + nodeTemplateName);
        }

        Capability capabilityTemplate = nodeTemplate.getCapabilities().get(capabilityId);
        IndexedCapabilityType indexedCapabilityType = csarRepoSearch.getRequiredElementInDependencies(IndexedCapabilityType.class,
                capabilityTemplate.getType(), topology.getDependencies());
        if (indexedCapabilityType.getProperties() == null || !indexedCapabilityType.getProperties().containsKey(propertyId)) {
            throw new NotFoundException("Property " + propertyId + " do not exist for capability " + capabilityId + " of node " + nodeTemplateName);
        }

        return topology.getOutputCapabilityProperties();
    }

    @ApiOperation(value = "Activate a capability property as an output property.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/property/{propertyId}/isOutput", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addOutputCapabilityProperty(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String propertyId, @PathVariable String capabilityId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, Map<String, Set<String>>> outputCapabilityProperties = getOutputCapabilityPropertiesOrThrowException(topology, nodeTemplateName,
                propertyId, capabilityId);
        if (outputCapabilityProperties == null) {
            Set<String> outputProperties = Sets.newHashSet(propertyId);
            Map<String, Set<String>> capabilityOutputProperties = Maps.newHashMap();
            capabilityOutputProperties.put(capabilityId, outputProperties);
            outputCapabilityProperties = Maps.newHashMap();
            outputCapabilityProperties.put(nodeTemplateName, capabilityOutputProperties);
        } else if (!outputCapabilityProperties.containsKey(nodeTemplateName)) {
            Set<String> outputProperties = Sets.newHashSet(propertyId);
            Map<String, Set<String>> capabilityOutputProperties = Maps.newHashMap();
            capabilityOutputProperties.put(capabilityId, outputProperties);
            outputCapabilityProperties.put(nodeTemplateName, capabilityOutputProperties);
        } else if (!outputCapabilityProperties.get(nodeTemplateName).containsKey(capabilityId)) {
            Set<String> outputProperties = Sets.newHashSet(propertyId);
            Map<String, Set<String>> capabilityOutputProperties = outputCapabilityProperties.get(nodeTemplateName);
            capabilityOutputProperties.put(capabilityId, outputProperties);
            outputCapabilityProperties.put(nodeTemplateName, capabilityOutputProperties);
        } else if (!outputCapabilityProperties.get(nodeTemplateName).get(capabilityId).contains(propertyId)) {
            outputCapabilityProperties.get(nodeTemplateName).get(capabilityId).add(propertyId);
        } else {
            // the property is already set as an output property
            return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
        }

        topology.setOutputCapabilityProperties(outputCapabilityProperties);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove a capability property from the output property list.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/capability/{capabilityId}/property/{propertyId}/isOutput", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeOutputCapabilityProperty(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String capabilityId, @PathVariable String propertyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, Map<String, Set<String>>> outputCapabilityProperties = getOutputCapabilityPropertiesOrThrowException(topology, nodeTemplateName,
                propertyId, capabilityId);
        if (outputCapabilityProperties == null || !outputCapabilityProperties.containsKey(nodeTemplateName)
                || !outputCapabilityProperties.get(nodeTemplateName).containsKey(capabilityId)
                || !outputCapabilityProperties.get(nodeTemplateName).get(capabilityId).contains(propertyId)) {
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.NOT_FOUND_ERROR).build()).build();
        }

        outputCapabilityProperties.get(nodeTemplateName).get(capabilityId).remove(propertyId);

        topology.setOutputCapabilityProperties(outputCapabilityProperties);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Activate an attribute as an output attribute.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/attributes/{attributeName}/output", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addOutputAttribute(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String attributeName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);

        if (nodeTemplate.getAttributes() != null && nodeTemplate.getAttributes().containsKey(attributeName)) {
            topology.setOutputAttributes(addToMap(topology.getOutputAttributes(), nodeTemplateName, attributeName));
        } else {
            // attributeName does not exists in the node template
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_MISSING_ERROR).build()).build();
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove a property from the output property list.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/property/{propertyName}/isOutput", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeOutputProperty(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String propertyName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        topology.setOutputProperties(removeValueFromMap(topology.getOutputProperties(), nodeTemplateName, propertyName));
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove an attribute from the output attributes list.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/attributes/{attributeName}/output", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeOutputAttribute(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String attributeName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        topology.setOutputAttributes(removeValueFromMap(topology.getOutputAttributes(), nodeTemplateName, attributeName));
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Add an artifact in the input artifact list.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/artifact/{artifactName}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addInputArtifact(@PathVariable String topologyId, @PathVariable String nodeTemplateName, @PathVariable String artifactName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);

        if (nodeTemplate.getArtifacts() != null && nodeTemplate.getArtifacts().containsKey(artifactName)) {
            topology.setInputArtifacts(addToMap(topology.getInputArtifacts(), nodeTemplateName, artifactName));
        } else {
            // attributeName does not exists in the node template
            return RestResponseBuilder.<TopologyDTO> builder().error(RestErrorBuilder.builder(RestErrorCode.PROPERTY_MISSING_ERROR).build()).build();
        }
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove an artifact from the input artifact list.", notes = "Returns a response with no errors and no data in success case. Application role required [ APPLICATION_MANAGER | ARCHITECT ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/artifact/{artifactName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeInputArtifact(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String artifactName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        topology.setInputArtifacts(removeValueFromMap(topology.getInputArtifacts(), nodeTemplateName, artifactName));
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    /**
     * Update the name of a relationship.
     *
     * @param topologyId The id of the topology in which the related node template lies.
     * @param nodeTemplateName The name of the node template in which is the relationship to rename.
     * @param relationshipName The old name of the relationship to rename.
     * @param newRelationshipName The new name of the relationship
     * @return {@link RestResponse}<{@link String}> an response with the new relationship name as data and no error if successful.
     */
    @ApiOperation(value = "Change the name of a node template in a topology.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodetemplates/{nodeTemplateName}/relationships/{relationshipName}/updateName", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> updateRelationshipName(@PathVariable String topologyId, @PathVariable String nodeTemplateName,
            @PathVariable String relationshipName, @RequestParam(value = "newName") String newRelationshipName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplate(topologyId, nodeTemplateName, nodeTemplates);

        Map<String, RelationshipTemplate> relationships = nodeTemplate.getRelationships();
        if (relationships == null || relationships.get(relationshipName) == null) {
            throw new NotFoundException("Node template [" + nodeTemplateName + "] do not have the relationship [" + relationshipName + "].");
        }

        isUniqueRelationshipName(topologyId, nodeTemplateName, newRelationshipName, relationships.keySet());

        relationships.put(newRelationshipName, relationships.get(relationshipName));
        relationships.remove(relationshipName);

        log.debug("Renaiming the relationship <{}> with <{}> in the node template <{}> of topology <{}> .", relationshipName, newRelationshipName,
                nodeTemplateName, topologyId);

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodeGroups/{groupName}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> updateGroupName(@PathVariable String topologyId, @PathVariable String groupName,
            @RequestParam(value = "newName") String newGroupName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        if (groupName.equals(newGroupName)) {
            return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
        }

        if (topology.getGroups().containsKey(newGroupName)) {
            throw new AlreadyExistException("Group with name [" + newGroupName + "] already exists, please choose another name");
        }

        NodeGroup nodeGroup = topology.getGroups().remove(groupName);
        if (nodeGroup != null) {
            nodeGroup.setName(newGroupName);
            Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
            for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
                if (nodeTemplate.getGroups() != null) {
                    if (nodeTemplate.getGroups().remove(groupName)) {
                        nodeTemplate.getGroups().add(newGroupName);
                    }
                }
            }
            topology.getGroups().put(newGroupName, nodeGroup);
        }

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    private int getAvailableGroupIndex(Topology topology) {
        Collection<NodeGroup> nodeGroups = topology.getGroups().values();
        LinkedHashSet<Integer> indexSet = new LinkedHashSet<>(nodeGroups.size());
        for (int i = 0; i < nodeGroups.size(); i++) {
            indexSet.add(i);
        }
        for (NodeGroup nodeGroup : nodeGroups)
            indexSet.remove(nodeGroup.getIndex());
        if (indexSet.isEmpty()) {
            return nodeGroups.size();
        }
        return indexSet.iterator().next();
    }

    @ApiOperation(value = "", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodeGroups/{groupName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> deleteNodeGroup(@PathVariable String topologyId, @PathVariable String groupName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        NodeGroup nodeGroup = topology.getGroups().remove(groupName);
        if (nodeGroup != null) {
            Map<String, NodeTemplate> nodeTemplates = topologyServiceCore.getNodeTemplates(topology);
            for (NodeTemplate nodeTemplate : nodeTemplates.values()) {
                if (nodeTemplate.getGroups() != null) {
                    nodeTemplate.getGroups().remove(groupName);
                }
            }
        }

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Add a node to a node group. If the group doesn't exists, it's created.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodeGroups/{groupName}/members/{nodeName}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> addNodeGroupMember(@PathVariable String topologyId, @PathVariable String groupName, @PathVariable String nodeName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);
        Map<String, NodeGroup> groups = topology.getGroups();
        if (groups == null) {
            groups = Maps.newHashMap();
            topology.setGroups(groups);
        }
        NodeGroup nodeGroup = groups.get(groupName);
        if (nodeGroup == null) {
            nodeGroup = new NodeGroup();
            nodeGroup.setName(groupName);
            nodeGroup.setIndex(getAvailableGroupIndex(topology));
            Set<String> members = Sets.newHashSet();
            nodeGroup.setMembers(members);
            List<AbstractPolicy> policies = Lists.newArrayList();
            // For the moment, groups are created only for HA
            AbstractPolicy policy = new HaPolicy();
            policy.setName("High Availability");
            policies.add(policy);
            nodeGroup.setPolicies(policies);
            groups.put(groupName, nodeGroup);
        }
        if (topology.getNodeTemplates() == null || !topology.getNodeTemplates().containsKey(nodeName)) {
            throw new NotFoundException("Attempt to add a non existing node [" + nodeName + "] to the group [" + groupName + "]");
        }
        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplates(topology).get(nodeName);
        if (nodeTemplate == null) {
            throw new NotFoundException("Attempt to add a non existing node [" + nodeName + "] to the group [" + groupName + "]");
        }
        if (nodeTemplate.getGroups() == null) {
            nodeTemplate.setGroups(Sets.<String> newHashSet());
        }
        nodeTemplate.getGroups().add(groupName);
        nodeGroup.getMembers().add(nodeName);
        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    @ApiOperation(value = "Remove a node from a node group.", notes = "Returns a response with no errors in case of success. Application role required [ APPLICATION_MANAGER | APPLICATION_DEVOPS ]")
    @RequestMapping(value = "/{topologyId:.+}/nodeGroups/{groupName}/members/{nodeName}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<TopologyDTO> removeNodeGroupMember(@PathVariable String topologyId, @PathVariable String groupName, @PathVariable String nodeName) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);
        topologyService.throwsErrorIfReleased(topology);

        NodeGroup nodeGroup = topology.getGroups().get(groupName);
        if (nodeGroup != null && nodeGroup.getMembers() != null) {
            nodeGroup.getMembers().remove(nodeName);
        }

        NodeTemplate nodeTemplate = topologyServiceCore.getNodeTemplates(topology).get(nodeName);
        if (nodeTemplate != null && nodeTemplate.getGroups() != null) {
            nodeTemplate.getGroups().remove(groupName);
        }

        alienDAO.save(topology);
        return RestResponseBuilder.<TopologyDTO> builder().data(topologyService.buildTopologyDTO(topology)).build();
    }

    private Map<String, Set<String>> addToMap(Map<String, Set<String>> map, String key, String value) {
        map = map == null ? new HashMap<String, Set<String>>() : map;

        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            map.put(key, Sets.newHashSet(value));
        }
        return map;
    }

    private Map<String, Set<String>> removeValueFromMap(Map<String, Set<String>> map, String key, String value) {
        if (map != null) {
            if (map.containsKey(key)) {
                map.get(key).remove(value);
                if (map.get(key).isEmpty()) {
                    map.remove(key);
                }
            }
        }
        return map;
    }

    @ApiOperation(value = "Get TOSCA plan/workflow to start the application.", authorizations = { @Authorization("ADMIN") })
    @RequestMapping(value = "/{topologyId:.+}/startplan", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RestResponse<StartEvent> getStartPlan(@PathVariable String topologyId) {
        Topology topology = topologyServiceCore.getMandatoryTopology(topologyId);
        topologyService.checkEditionAuthorizations(topology);

        Map<String, PaaSNodeTemplate> nodeTemplates = topologyTreeBuilderService.buildPaaSNodeTemplate(topology);
        List<PaaSNodeTemplate> roots = topologyTreeBuilderService.buildPaaSTopology(nodeTemplates).getComputes();

        StartEvent startEvent = new BuildPlanGenerator(true).generate(roots);

        return RestResponseBuilder.<StartEvent> builder().data(startEvent).build();
    }
}
