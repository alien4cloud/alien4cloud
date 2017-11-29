package org.alien4cloud.tosca.editor.services;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import alien4cloud.utils.CloneUtil;
import alien4cloud.utils.FileUtil;
import com.google.common.collect.Sets;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.catalog.index.ICsarDependencyLoader;
import org.alien4cloud.tosca.catalog.index.IToscaTypeIndexerService;
import org.alien4cloud.tosca.catalog.repository.ICsarRepositry;
import org.alien4cloud.tosca.editor.events.SubstitutionTypeChangedEvent;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AttributeDefinition;
import org.alien4cloud.tosca.model.definitions.CapabilityDefinition;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.RequirementDefinition;
import org.alien4cloud.tosca.model.definitions.ScalarPropertyValue;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.SubstitutionTarget;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.elasticsearch.common.collect.Lists;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.model.components.IndexedModelUtils;
import org.alien4cloud.tosca.utils.TopologyUtils;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import org.alien4cloud.tosca.normative.types.ToscaTypes;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages topology substitutions
 */
@Service
@Slf4j
public class TopologySubstitutionService {
    @Inject
    private CsarService csarService;
    @Inject
    private ICsarRepositry csarRepositry;
    @Inject
    private IToscaTypeIndexerService indexerService;
    @Inject
    private ICsarDependencyLoader csarDependencyLoader;
    @Inject
    private ApplicationEventPublisher publisher;

    @ToscaContextual
    public void updateSubstitutionType(final Topology topology, Csar csar) {
        if (topology.getSubstitutionMapping() == null || topology.getSubstitutionMapping().getSubstitutionType() == null) {
            return;
        }

        // first we update the csar and the dependencies
        NodeType nodeType = ToscaContext.getOrFail(NodeType.class, topology.getSubstitutionMapping().getSubstitutionType());
        if (csar.getDependencies() == null) {
            csar.setDependencies(Sets.newHashSet());
        }
        boolean updateCsar = false;
        if (csar.getDependencies().add(csarDependencyLoader.buildDependencyBean(nodeType.getArchiveName(), nodeType.getArchiveVersion()))) {
            updateCsar = true;
        }
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        String hash = FileUtil.deepSHA1(archiveGitPath);
        if (!hash.equals(csar.getHash())) {
            csar.setHash(hash);
            updateCsar = true;
        }
        if (updateCsar) {
            csarService.save(csar);
        }

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

        // Dispatch event
        publisher.publishEvent(new SubstitutionTypeChangedEvent(this, topology, substituteNodeType));
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
                // We cannot change the capability definition here or we will change the original one so we need a clone
                requirementDefinition = CloneUtil.clone(requirementDefinition);
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
                // We cannot change the capability definition here or we will change the original one so we need a clone
                capabilityDefinition = CloneUtil.clone(capabilityDefinition);
                capabilityDefinition.setId(key);
                substituteNodeType.getCapabilities().add(capabilityDefinition);
            }
        }
    }

    private void fillAttributesFromOutputCapabilitiesProperties(Topology topology, NodeType substituteNodeType) {
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
                        // there is a conflict
                        addAttributeFromPropertyDefinition(pd, propertyName, substituteNodeType);
                    }
                }
            }
        }
    }

    private void fillSubstituteAttributesFromOutputProperties(Topology topology, NodeType substituteNodeType) {
        Map<String, Set<String>> outputProperties = topology.getOutputProperties();
        if (outputProperties != null) {
            for (Map.Entry<String, Set<String>> ope : outputProperties.entrySet()) {
                String nodeName = ope.getKey();
                NodeTemplate nodeTemplate = topology.getNodeTemplates().get(nodeName);
                NodeType nodeTemplateType = ToscaContext.getOrFail(NodeType.class, nodeTemplate.getType());
                for (String propertyName : ope.getValue()) {
                    PropertyDefinition pd = nodeTemplateType.getProperties().get(propertyName);
                    // is a conflict
                    addAttributeFromPropertyDefinition(pd, propertyName, substituteNodeType);
                }
            }
        }
    }

    private void addAttributeFromPropertyDefinition(PropertyDefinition pd, String propertyName, NodeType substituteNodeType) {
        // FIXME we have an issue here : if several nodes have the same attribute name, or if an attribute and a property have the same name,
        Map<String, IValue> attributes = substituteNodeType.getAttributes();
        if (pd != null && !attributes.containsKey(propertyName)) {
            if (ToscaTypes.isSimple(pd.getType())) {
                AttributeDefinition attributeDefinition = new AttributeDefinition();
                attributeDefinition.setType(pd.getType());
                attributeDefinition.setDescription(pd.getDescription());
                // FIXME known issue we don't support complex attributes right now.
                if (pd.getDefault() != null && pd.getDefault() instanceof ScalarPropertyValue) {
                    attributeDefinition.setDefault(((ScalarPropertyValue) pd.getDefault()).getValue());
                }
                attributes.put(propertyName, attributeDefinition);
            } // FIXME else: known issue we don't support complex attributes right now.
        }
    }

    private void fillSubstituteAttributesFromTypeAtttributes(Topology topology, NodeType substituteNodeType) {
        Map<String, IValue> attributes = substituteNodeType.getAttributes();
        Map<String, Set<String>> outputAttributes = topology.getOutputAttributes();
        if (outputAttributes != null) {
            for (Map.Entry<String, Set<String>> oae : outputAttributes.entrySet()) {
                String nodeName = oae.getKey();
                NodeTemplate nodeTemplate = TopologyUtils.getNodeTemplate(topology, nodeName);
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
