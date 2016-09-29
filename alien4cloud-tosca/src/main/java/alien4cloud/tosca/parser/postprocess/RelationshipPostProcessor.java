package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.topology.NodeTemplateBuilder;

@Component
public class RelationshipPostProcessor {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;
    @Resource
    private ArtifactPostProcessor artifactPostProcessor;

    public void process(NodeType nodeTemplateType, Map.Entry<String, RelationshipTemplate> instance) {
        RelationshipTemplate relationshipTemplate = instance.getValue();
        if (relationshipTemplate.getTarget() == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(instance);
            // the node template name is required
            ParsingContextExecution.getParsingErrors()
                    .add(new ParsingError(ErrorCode.REQUIREMENT_TARGET_NODE_TEMPLATE_NAME_REQUIRED, null, node.getStartMark(), null, node.getEndMark(), null));
        }
        RelationshipType relationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
        propertyValueChecker.checkProperties(relationshipType, relationshipTemplate.getProperties(), instance.getKey());

        RequirementDefinition rd = getRequirementDefinitionByName(nodeTemplateType, relationshipTemplate.getRequirementName());
        if (rd == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(relationshipTemplate.getRequirementName());
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_NOT_FOUND, null, node.getStartMark(), null, node.getEndMark(),
                    relationshipTemplate.getRequirementName()));
            return;
        }
        if (relationshipTemplate.getType() == null) {
            // if the relationship type has not been defined on the requirement assignment it may be defined on the requirement definition.
            relationshipTemplate.setType(rd.getRelationshipType());
        }
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(relationshipTemplate.getType(), RelationshipType.class));
        relationshipTemplate.setRequirementType(rd.getType());

        ArchiveRoot archiveRoot = (ArchiveRoot) ParsingContextExecution.getRoot().getWrappedInstance();
        // now find the target of the relation
        NodeTemplate targetNodeTemplate = archiveRoot.getTopology().getNodeTemplates().get(relationshipTemplate.getTarget());
        if (targetNodeTemplate == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(relationshipTemplate.getTarget());
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_TARGET_NOT_FOUND, null, node.getStartMark(), null,
                    node.getEndMark(), relationshipTemplate.getTarget()));
            return;
        }

        String capabilityType = relationshipTemplate.getTargetedCapabilityName(); // alien actually supports a capability type in the TOSCA yaml
        Capability capability = null;
        if (capabilityType == null) {
            // the capability type is not known, we assume that we are parsing a Short notation (node only)
            // in such notation : "a requirement named ‘host’ that needs to be fulfilled by the same named capability"
            // so here we use the requirement name to find the capability
            if (targetNodeTemplate.getCapabilities() != null) {
                capability = targetNodeTemplate.getCapabilities().get(relationshipTemplate.getRequirementName());
                if (capability != null) {
                    relationshipTemplate.setTargetedCapabilityName(rd.getId());
                }
            }
        } else {
            Map.Entry<String, Capability> capabilityEntry = getCapabilityByType(targetNodeTemplate, capabilityType);
            if (capabilityEntry != null) {
                capability = capabilityEntry.getValue();
                relationshipTemplate.setTargetedCapabilityName(capabilityEntry.getKey());
            }
        }
        if (capability == null) {
            Node node = ParsingContextExecution.getObjectToNodeMap().get(relationshipTemplate);
            // we should fail
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ErrorCode.REQUIREMENT_CAPABILITY_NOT_FOUND, null, node.getStartMark(), null,
                    node.getEndMark(), relationshipTemplate.getRequirementName()));
            return;
        }

        RelationshipType indexedRelationshipType = ToscaContext.get(RelationshipType.class, relationshipTemplate.getType());
        if (indexedRelationshipType == null) {
            // Error managed by the reference post processor.
            return;
        }
        Map<String, AbstractPropertyValue> properties = Maps.newLinkedHashMap();
        NodeTemplateBuilder.fillProperties(properties, indexedRelationshipType.getProperties(), relationshipTemplate.getProperties(), false);
        relationshipTemplate.setProperties(properties);
        relationshipTemplate.setAttributes(indexedRelationshipType.getAttributes());

        // FIXME we should check that the artifact is defined at the type level.
        safe(instance.getValue().getArtifacts()).values().forEach(artifactPostProcessor);
        Map<String, DeploymentArtifact> mergedArtifacts = instance.getValue().getArtifacts();
        if (mergedArtifacts == null) {
            mergedArtifacts = new HashMap<>();
        }
        mergedArtifacts.putAll(safe(indexedRelationshipType.getArtifacts()));
        relationshipTemplate.setArtifacts(mergedArtifacts);

        // TODO Manage interfaces inputs to copy them to all operations.
        for (Interface anInterface : safe(instance.getValue().getInterfaces()).values()) {
            safe(anInterface.getOperations()).values().stream().map(Operation::getImplementationArtifact).filter(Objects::nonNull)
                    .forEach(artifactPostProcessor);
        }
    }

    private Map.Entry<String, Capability> getCapabilityByType(NodeTemplate nodeTemplate, String type) {
        if (nodeTemplate.getCapabilities() == null) { // add a check in case the node doesn't have capabilities
            return null;
        }
        for (Map.Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
            if (type.equals(capabilityEntry.getValue().getType())) {
                return capabilityEntry;
            }
        }
        return null;
    }

    private RequirementDefinition getRequirementDefinitionByName(NodeType indexedNodeType, String name) {
        if (indexedNodeType.getRequirements() != null) {
            for (RequirementDefinition rd : indexedNodeType.getRequirements()) {
                if (rd.getId().equals(name)) {
                    return rd;
                }
            }
        }
        return null;
    }
}