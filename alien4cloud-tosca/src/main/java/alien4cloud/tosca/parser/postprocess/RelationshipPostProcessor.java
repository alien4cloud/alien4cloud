package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.definitions.*;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.RelationshipTemplate;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import com.google.common.collect.Maps;

import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.topology.TemplateBuilder;

@Component
public class RelationshipPostProcessor {
    @Resource
    private ReferencePostProcessor referencePostProcessor;
    @Resource
    private PropertyValueChecker propertyValueChecker;
    @Resource
    private TemplateDeploymentArtifactPostProcessor templateDeploymentArtifactPostProcessor;
    @Resource
    private ImplementationArtifactPostProcessor implementationArtifactPostProcessor;
    @Resource
    private ICapabilityMatcherService capabilityMatcherService;

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
        referencePostProcessor.process(new ReferencePostProcessor.TypeReference(relationshipTemplate, relationshipTemplate.getType(), RelationshipType.class));
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

        String capabilityStr = relationshipTemplate.getTargetedCapabilityName(); // alien actually supports a capability type in the TOSCA yaml
        Capability capability = null;
        if (capabilityStr == null) {
            // the capability type is not known, we assume that we are parsing a Short notation (node only)
            if (targetNodeTemplate.getCapabilities() != null) {
                // let's try to find all match for a given type
                capability = getCapabilityByType(targetNodeTemplate, relationshipTemplate, relationshipTemplate.getRequirementType());
                if (capability == null) {
                    capability = targetNodeTemplate.getCapabilities().get(relationshipTemplate.getRequirementName());
                    if (capability != null) {
                        relationshipTemplate.setTargetedCapabilityName(rd.getId());
                    }
                }
            }
        } else {
            // Let's try to find if the target node has a capability as named in the capability string of the relationship (requirement assignment)
            if (targetNodeTemplate.getCapabilities() != null) {
                capability = targetNodeTemplate.getCapabilities().get(capabilityStr);
            }
            if (capability == null) {
                // The capabilityStr may be the name of a type
                capability = getCapabilityByType(targetNodeTemplate, relationshipTemplate, capabilityStr);
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
        TemplateBuilder.fillProperties(properties, indexedRelationshipType.getProperties(), relationshipTemplate.getProperties(), false);
        relationshipTemplate.setProperties(properties);
        relationshipTemplate.setAttributes(indexedRelationshipType.getAttributes());

        // FIXME we should check that the artifact is defined at the type level.
        safe(instance.getValue().getArtifacts()).values().forEach(templateDeploymentArtifactPostProcessor);
        Map<String, DeploymentArtifact> mergedArtifacts = instance.getValue().getArtifacts();
        if (mergedArtifacts == null) {
            mergedArtifacts = new HashMap<>();
        }
        mergedArtifacts.putAll(safe(indexedRelationshipType.getArtifacts()));
        relationshipTemplate.setArtifacts(mergedArtifacts);

        // TODO Manage interfaces inputs to copy them to all operations.
        for (Interface anInterface : safe(instance.getValue().getInterfaces()).values()) {
            safe(anInterface.getOperations()).values().stream().map(Operation::getImplementationArtifact).filter(Objects::nonNull)
                    .forEach(implementationArtifactPostProcessor);
        }
    }

    private Capability getCapabilityByType(NodeTemplate targetNodeTemplate, RelationshipTemplate relationshipTemplate, String capabilityType) {
        Capability capability = null;
        Map<String, Capability> compatibleCapabilityByType = capabilityMatcherService.getCompatibleCapabilityByType(targetNodeTemplate, capabilityType);
        Entry<String, Capability> capabilityEntry = null;
        if (compatibleCapabilityByType.size() == 1) {
            capabilityEntry = compatibleCapabilityByType.entrySet().iterator().next();
        } else if (compatibleCapabilityByType.size() > 1) {
            capabilityEntry = compatibleCapabilityByType.entrySet().iterator().next();
            Node node = ParsingContextExecution.getObjectToNodeMap().get(relationshipTemplate);
            ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.REQUIREMENT_CAPABILITY_MULTIPLE_MATCH, null,
                    node.getStartMark(), null, node.getEndMark(), relationshipTemplate.getRequirementName()));
        }
        if (capabilityEntry != null) {
            capability = capabilityEntry.getValue();
            relationshipTemplate.setTargetedCapabilityName(capabilityEntry.getKey());
        }
        return capability;
    }

    private RequirementDefinition getRequirementDefinitionByName(NodeType indexedNodeType, String name) {
        if (indexedNodeType.getRequirements() != null) {
            for (RequirementDefinition rd : indexedNodeType.getRequirements()) {
                // requirement definition id may be null in case of a wrong defined node type in the same archive.
                if (rd.getId() != null && rd.getId().equals(name)) {
                    return rd;
                }
            }
        }
        return null;
    }
}