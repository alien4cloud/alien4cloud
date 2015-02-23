package alien4cloud.tosca.parser.impl.advanced;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.csar.services.CsarService;
import alien4cloud.model.components.AbstractPropertyValue;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.model.components.RequirementDefinition;
import alien4cloud.model.topology.Capability;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ToscaParsingUtil;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.MapParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;

import com.google.common.collect.Maps;

@Component
@Slf4j
public class RelationshipTemplatesParser extends DefaultDeferredParser<Map<String, RelationshipTemplate>> {

    @Resource
    private CsarService csarService;

    @Resource
    private ScalarParser scalarParser;

    @Resource
    private ICSARRepositorySearchService searchService;

    @Resource
    private TopologyServiceCore topologyServiceCore;

    @Override
    public Map<String, RelationshipTemplate> parse(Node node, ParsingContextExecution context) {
        Object parent = context.getParent();
        if (!(parent instanceof NodeTemplate)) {
            // TODO: throw ex
        }
        NodeTemplate nodeTemplate = (NodeTemplate) parent;
        Map<String, RelationshipTemplate> result = new HashMap<String, RelationshipTemplate>();
        if (!(node instanceof SequenceNode)) {
            // we expect a SequenceNode
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaSequenceExpected", node.getStartMark(), "a YAML SequenceNode is expected here", node
                            .getEndMark(), ""));
            return null;
        }
        SequenceNode mappingNode = ((SequenceNode) node);
        List<Node> children = mappingNode.getValue();
        for (Node child : children) {
            if (!(child instanceof MappingNode)) {
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.VALIDATION_ERROR, "ToscaMappingNodeExpected", child.getStartMark(),
                                "a YAML MappingNode is expected here, ignoring this node", child.getEndMark(), ""));
                continue;
            }
            MappingNode requirementNode = (MappingNode) child;
            List<NodeTuple> requirementNodeTuples = requirementNode.getValue();
            NodeTuple nt = requirementNodeTuples.get(0);
            Node keyNode = nt.getKeyNode();
            // can a key be something else than a scalar node ?
            String toscaRequirementName = scalarParser.parse(keyNode, context);
            Node valueNode = nt.getValueNode();
            if (valueNode instanceof ScalarNode) {
                // the value node is a scalar, this is the short notation for requirements : Short notation (node only)
                // ex: host: compute
                String toscaRequirementTargetNodeTemplateName = ((ScalarNode) valueNode).getValue();
                buildAndAddRelationhip(valueNode, nodeTemplate, toscaRequirementName, toscaRequirementTargetNodeTemplateName, null, null, context, result, null);
            } else if (valueNode instanceof MappingNode) {
                // the value is not a scalar, Short notation (with relationship or capability) or Extended notation
                // we only parser the Short notation (with relationship or capability)
                MappingNode mappingValueNode = (MappingNode) valueNode;
                mappingValueNode.getValue();

                // let's search for requirement's properties
                Map<String, AbstractPropertyValue> relationshipProperties = null;
                for (NodeTuple mappingValueNodeChilds : mappingValueNode.getValue()) {
                    if (mappingValueNodeChilds.getKeyNode() instanceof ScalarNode
                            && ((ScalarNode) mappingValueNodeChilds.getKeyNode()).getValue().equals("properties")
                            && (mappingValueNodeChilds.getValueNode() instanceof MappingNode)) {
                        INodeParser<AbstractPropertyValue> propertyValueParser = context.getRegistry().get("property_value");
                        MapParser<AbstractPropertyValue> mapParser = new MapParser<AbstractPropertyValue>(propertyValueParser, "property_value");
                        relationshipProperties = mapParser.parse(mappingValueNodeChilds.getValueNode(), context);
                    }
                }

                Map<String, String> map = ParserUtils.parseStringMap(mappingValueNode, context, "properties");
                // for the node (node_type_or_template_name), we only accpet the "template_name"
                String toscaRequirementTargetNodeTemplateName = map.get("node");
                if (toscaRequirementTargetNodeTemplateName == null) {
                    // the node template name is required
                    context.getParsingErrors().add(
                            new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaRequirementTargetNodeTemplateNameRequired", valueNode.getStartMark(),
                                    "The requirement target node should be defined using a 'node' property, not able to find the relation ship target",
                                    valueNode.getEndMark(), ""));
                    continue;
                }
                // this is a Capability Type
                String tosca_capability = map.get("capability");
                // this is a Relationship Type
                String tosca_relationship = map.get("relationship");
                buildAndAddRelationhip(valueNode, nodeTemplate, toscaRequirementName, toscaRequirementTargetNodeTemplateName, tosca_capability,
                        tosca_relationship, context, result, relationshipProperties);
            }

        }
        return result;
    }

    private void buildAndAddRelationhip(Node node, NodeTemplate nodeTemplate, String toscaRequirementName, String toscaRequirementTargetNodeTemplateName,
            String capabilityType, String relationshipType, ParsingContextExecution context, Map<String, RelationshipTemplate> relationships,
            Map<String, AbstractPropertyValue> relationshipProperties) {
        RelationshipTemplate relationshipTemplate = buildRelationshipTemplate(node, nodeTemplate, toscaRequirementName, toscaRequirementTargetNodeTemplateName,
                capabilityType, relationshipType, context, relationshipProperties);
        if (relationshipTemplate == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.VALIDATION_ERROR, "ToscaRelationshipNotBuilt", node.getStartMark(),
                            "A relationship has been skipped, checks the errors", node.getEndMark(), toscaRequirementName));
        } else {
            String relationShipName = buildRelationShipTemplateName(relationshipTemplate, toscaRequirementTargetNodeTemplateName);
            addRelationshipTemplateToMap(relationships, relationShipName, relationshipTemplate, 0);
        }
    }

    private void addRelationshipTemplateToMap(Map<String, RelationshipTemplate> map, String name, RelationshipTemplate relationshipTemplate, int attempCount) {
        String key = name;
        if (attempCount > 0) {
            key += attempCount;
        }
        if (map.containsKey(key)) {
            addRelationshipTemplateToMap(map, name, relationshipTemplate, attempCount++);
        } else {
            map.put(key, relationshipTemplate);
        }
    }

    private String buildRelationShipTemplateName(RelationshipTemplate relationshipTemplate, String targetName) {
        String value = relationshipTemplate.getType();
        if (value.contains(".")) {
            value = value.substring(value.lastIndexOf(".") + 1);
        }
        value = StringUtils.uncapitalize(value);
        value = value + StringUtils.capitalize(targetName);
        return value;
    }

    private RelationshipTemplate buildRelationshipTemplate(Node node, NodeTemplate nodeTemplate, String toscaRequirementName,
            String toscaRequirementTargetNodeTemplateName, String capabilityType, String relationshipType, ParsingContextExecution context,
            Map<String, AbstractPropertyValue> relationshipProperties) {
        RelationshipTemplate relationshipTemplate = new RelationshipTemplate();
        ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
        IndexedNodeType indexedNodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(nodeTemplate.getType(), archiveRoot, searchService);
        if (indexedNodeType == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.TYPE_NOT_FOUND, "node_template requirements parsing", node.getStartMark(),
                            "Not able to find the note template's type nor in current or dependencies", node.getEndMark(), nodeTemplate.getType()));
            return null;
        }
        RequirementDefinition rd = getRequirementDefinitionByName(indexedNodeType, toscaRequirementName);
        if (rd == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaRequirementNotFound", node.getStartMark(),
                            "Not able to find the requirement definition in node type", node.getEndMark(), toscaRequirementName));
            return null;
        }
        // ex: host
        relationshipTemplate.setRequirementName(toscaRequirementName);
        // relationshipTemplate.setTargetedCapabilityName(rd.getId());
        // ex: tosca.nodes.Compute
        relationshipTemplate.setRequirementType(rd.getType());
        // ex: tosca.relationships.HostedOn
        relationshipTemplate.setType(rd.getRelationshipType());

        // now find the target of the relation
        NodeTemplate targetNodeTemplate = archiveRoot.getTopology().getNodeTemplates().get(toscaRequirementTargetNodeTemplateName);
        if (targetNodeTemplate == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaRequirementTargetNotFound", node.getStartMark(),
                            "The target of the requirement can not be found, the relationship can not be added", node.getEndMark(),
                            toscaRequirementTargetNodeTemplateName));
            return null;
        }
        IndexedNodeType indexedTargetNodeType = ToscaParsingUtil.getNodeTypeFromArchiveOrDependencies(targetNodeTemplate.getType(), archiveRoot, searchService);
        if (!indexedTargetNodeType.getDerivedFrom().contains(rd.getType())) {
            // an error ?
            // context.getParsingErrors().add(
            // new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.VALIDATION_ERROR, "node_template requirements parsing", node.getStartMark(),
            // "The relation target doesn't seem to be compibatble with the requirement", node.getEndMark(), targetNodeTemplate.getType()));
        }

        Capability capability = null;
        if (capabilityType == null) {
            // the capability type is not known, we assume that we are parsing a Short notation (node only)
            // in such notation : "a requirement named ‘host’ that needs to be fulfilled by the same named capability"
            // so here we use the requirement name to find the capability
            capability = targetNodeTemplate.getCapabilities().get(toscaRequirementName);
            if (capability != null) {
                relationshipTemplate.setTargetedCapabilityName(rd.getId());
            }
        } else {
            Entry<String, Capability> capabilityEntry = getCapabilityByType(targetNodeTemplate, capabilityType);
            if (capabilityEntry != null) {
                capability = capabilityEntry.getValue();
                relationshipTemplate.setTargetedCapabilityName(capabilityEntry.getKey());
            }
        }
        if (capability == null) {
            // we should fail
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaRequirementCapabilityNotFound", node.getStartMark(),
                            "The capability target of the requierement can not be identified", node.getEndMark(), toscaRequirementName));
            return null;
        }

        relationshipTemplate.setTarget(toscaRequirementTargetNodeTemplateName);

        // now find the relationship type
        IndexedRelationshipType indexedRelationshipType = ToscaParsingUtil.getRelationshipTypeFromArchiveOrDependencies(rd.getRelationshipType(), archiveRoot,
                searchService);
        if (indexedRelationshipType == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.TYPE_NOT_FOUND, "ToscaRequirementRelationshipTypeNotFound", node.getStartMark(),
                            "The relationship type corresponding to the requirement definition can't be identified", node.getEndMark(), rd
                                    .getRelationshipType()));
            return null;
        }
        Map<String, AbstractPropertyValue> properties = Maps.newHashMap();
        TopologyServiceCore.fillProperties(properties, indexedRelationshipType.getProperties(), relationshipProperties);
        relationshipTemplate.setProperties(properties);
        return relationshipTemplate;
    }

    private Entry<String, Capability> getCapabilityByType(NodeTemplate nodeTemplate, String type) {
        for (Entry<String, Capability> capabilityEntry : nodeTemplate.getCapabilities().entrySet()) {
            if (capabilityEntry.getValue().getType().equals(type)) {
                return capabilityEntry;
            }
        }
        return null;
    }

    private RequirementDefinition getRequirementDefinitionByName(IndexedNodeType indexedNodeType, String name) {
        for (RequirementDefinition rd : indexedNodeType.getRequirements()) {
            if (rd.getId().equals(name)) {
                return rd;
            }
        }
        return null;
    }

}