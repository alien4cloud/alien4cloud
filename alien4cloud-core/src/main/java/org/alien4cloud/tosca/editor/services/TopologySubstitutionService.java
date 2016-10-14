package org.alien4cloud.tosca.editor.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;

import org.alien4cloud.tosca.catalog.ArchiveDelegateType;
import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.catalog.index.IToscaTypeIndexerService;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.common.collect.Lists;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.model.components.IndexedModelUtils;
import alien4cloud.topology.TopologyServiceCore;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages topology substitutions
 */
@Service
@Slf4j
public class TopologySubstitutionService {

    @Resource
    private ICsarService csarService;
    @Resource
    private IToscaTypeIndexerService indexerService;
    @Resource
    private ICsarDependencyLoader csarDependencyLoader;

    @ToscaContextual
    public void updateSubstitutionType(final Topology topology, Csar csar) {
        // FIXME we do not yet support substitution from application topology
        if (Objects.equals(csar.getDelegateType(), ArchiveDelegateType.APPLICATION)) {
            return;
        }
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            return;
        }

        // first we update the csar and the dependencies
        NodeType nodeType = ToscaContext.getOrFail(NodeType.class, topology.getSubstitutionMapping().getSubstitutionType().getElementId());
        csar.getDependencies().add(csarDependencyLoader.buildDependencyBean(nodeType.getArchiveName(), nodeType.getArchiveVersion()));
        // FIXME manage hash for substitution elements too (should we just generate based on type).
        // csar.setHash("-1");
        csarService.save(csar);

        // We create the nodeType that will serve as substitute
        NodeType substituteNodeType = buildSubstituteNodeType(topology, csar, nodeType);

        // inputs from topology become properties of type
        substituteNodeType.setProperties(topology.getInputs());

        // output attributes become attributes for the type
        fillSubstituteAttributesFromTypeAtttributes(topology, substituteNodeType);

        // output properties become attributes for the type
        fillSubstituteAttributesFromOutputProperties(topology, substituteNodeType);

        // output capabilities properties also become attributes for the type
        fillAttributesFromOutputCapabilitiesProperties(topology, substituteNodeType);

        // capabilities substitution
        fillCapabilities(topology, substituteNodeType);

        // requirement substitution
        fillRequirements(topology, substituteNodeType);

        // finally we index the created type
        indexerService.indexInheritableElement(csar.getName(), csar.getVersion(), substituteNodeType, csar.getDependencies());
    }

    private void fillRequirements(Topology topology, NodeType substituteNodeType) {
        if (topology.getSubstitutionMapping().getRequirements() != null) {
            for (Map.Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getRequirements().entrySet()) {
                String key = e.getKey();
                String nodeName = e.getValue().getNodeTemplateName();
                String requirementName = e.getValue().getTargetId();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                NodeType nodeTemplateType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
                RequirementDefinition requirementDefinition = IndexedModelUtils.getRequirementDefinitionById(nodeTemplateType.getRequirements(),
                        requirementName);
                requirementDefinition.setId(key);
                substituteNodeType.getRequirements().add(requirementDefinition);
            }
        }
    }

    private void fillCapabilities(Topology topology, NodeType substituteNodeType) {
        if (topology.getSubstitutionMapping().getCapabilities() != null) {
            for (Map.Entry<String, SubstitutionTarget> e : topology.getSubstitutionMapping().getCapabilities().entrySet()) {
                String key = e.getKey();
                String nodeName = e.getValue().getNodeTemplateName();
                String capabilityName = e.getValue().getTargetId();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                NodeType nodeTemplateType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
                CapabilityDefinition capabilityDefinition = IndexedModelUtils.getCapabilityDefinitionById(nodeTemplateType.getCapabilities(), capabilityName);
                capabilityDefinition.setId(key);
                substituteNodeType.getCapabilities().add(capabilityDefinition);
            }
        }
    }

    private void fillAttributesFromOutputCapabilitiesProperties(Topology topology, NodeType substituteNodeType) {
        Map<String, IValue> attributes = substituteNodeType.getAttributes();
        Map<String, Map<String, Set<String>>> outputCapabilityProperties = topology.getOutputCapabilityProperties();
        if (outputCapabilityProperties != null) {
            for (Map.Entry<String, Map<String, Set<String>>> ocpe : outputCapabilityProperties.entrySet()) {
                String nodeName = ocpe.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                for (Map.Entry<String, Set<String>> cpe : ocpe.getValue().entrySet()) {
                    String capabilityName = cpe.getKey();
                    String capabilityTypeName = nodeTemplate.getCapabilities().get(capabilityName).getType();
                    CapabilityType capabilityType = ToscaContext.getOrFail(CapabilityType.class, capabilityTypeName);
                    for (String propertyName : cpe.getValue()) {
                        PropertyDefinition pd = capabilityType.getProperties().get(propertyName);
                        // FIXME we have an issue here : if several nodes have the same attribute name, or if an attribute and a property have the same name,
                        // there is a conflict
                        if (pd != null && !attributes.containsKey(propertyName)) {
                            attributes.put(propertyName, pd);
                        }
                    }
                }
            }
        }
    }

    private void fillSubstituteAttributesFromOutputProperties(Topology topology, NodeType substituteNodeType) {
        Map<String, IValue> attributes = substituteNodeType.getAttributes();
        Map<String, Set<String>> outputProperties = topology.getOutputProperties();
        if (outputProperties != null) {
            for (Map.Entry<String, Set<String>> ope : outputProperties.entrySet()) {
                String nodeName = ope.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                NodeType nodeTemplateType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
                for (String propertyName : ope.getValue()) {
                    PropertyDefinition pd = nodeTemplateType.getProperties().get(propertyName);
                    // FIXME we have an issue here : if several nodes have the same attribute name, or if an attribute and a property have the same name, there
                    // is a conflict
                    if (pd != null && !attributes.containsKey(propertyName)) {
                        attributes.put(propertyName, pd);
                    }
                }
            }
        }
    }

    private void fillSubstituteAttributesFromTypeAtttributes(Topology topology, NodeType substituteNodeType) {
        Map<String, IValue> attributes = substituteNodeType.getAttributes();
        Map<String, Set<String>> outputAttributes = topology.getOutputAttributes();
        if (outputAttributes != null) {
            for (Map.Entry<String, Set<String>> oae : outputAttributes.entrySet()) {
                String nodeName = oae.getKey();
                NodeTemplate nodeTemplate = TopologyServiceCore.getNodeTemplate(topology, nodeName);
                NodeType nodeTemplateType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
                for (String attributeName : oae.getValue()) {
                    IValue ivalue = nodeTemplateType.getAttributes().get(attributeName);
                    // FIXME we have an issue here : if several nodes have the same attribute name, or if an attribute and a property have the same name, there
                    // is a conflict
                    if (ivalue != null && !attributes.containsKey(attributeName)) {
                        attributes.put(attributeName, ivalue);
                    }
                }
            }
        }
    }

    private NodeType buildSubstituteNodeType(Topology topology, Csar csar, NodeType nodeType) {
        NodeType substituteNodeType = new NodeType();
        substituteNodeType.setArchiveName(csar.getName());
        substituteNodeType.setArchiveVersion(csar.getVersion());
        substituteNodeType.setElementId(csar.getName());
        substituteNodeType.setDerivedFrom(Lists.newArrayList(nodeType.getElementId()));
        substituteNodeType.setSubstitutionTopologyId(topology.getId());
        substituteNodeType.setWorkspace(topology.getWorkspace());
        List<CapabilityDefinition> capabilities = Lists.newArrayList();
        substituteNodeType.setCapabilities(capabilities);
        List<RequirementDefinition> requirements = Lists.newArrayList();
        substituteNodeType.setRequirements(requirements);
        substituteNodeType.setAttributes(Maps.newHashMap());
        return substituteNodeType;
    }
}
