package alien4cloud.service;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.inject.Inject;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.instances.NodeInstance;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.rest.utils.PatchUtil;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.context.ToscaContextual;
import alien4cloud.tosca.parser.postprocess.NodeTemplatePostProcessor;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.tosca.topology.NodeTemplateBuilder;
import alien4cloud.utils.services.PropertyService;

/**
 * Simple service to manage node instance validations.
 */
@Service
public class NodeInstanceService {
    @Inject
    private NodeTemplatePostProcessor nodeTemplatePostProcessor;
    @Inject
    private PropertyService propertyService;

    /**
     * Create a new instance of a given node type based on default generated template.
     * 
     * @param nodeType The node type out of which to create the version.
     * @param typeVersion The node instance type's version.
     * @return An instance that matches the given type created from a default template (default values). Note that the node instance may be constructed from an
     *         invalid template (missing required properties) without errors. State of the node is set to initial.
     */
    @ToscaContextual
    public NodeInstance create(NodeType nodeType, String typeVersion) {
        NodeTemplate nodeTemplate = NodeTemplateBuilder.buildNodeTemplate(nodeType, null);
        NodeInstance instance = new NodeInstance();
        instance.setAttribute(ToscaNodeLifecycleConstants.ATT_STATE, ToscaNodeLifecycleConstants.INITIAL);
        instance.setNodeTemplate(nodeTemplate);
        instance.setTypeVersion(typeVersion);
        return instance;
    }

    @ToscaContextual
    public void patch(NodeType nodeType, NodeInstance nodeInstance, Map<String, AbstractPropertyValue> nodeProperties, Map<String, Capability> nodeCapabilities,
            Map<String, String> nodeAttributeValues) throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        if (nodeProperties != null) {
            nodeProperties = PatchUtil.realValue(nodeProperties);
            if (nodeProperties == null) {
                nodeInstance.getNodeTemplate().setProperties(Maps.newLinkedHashMap());
            } else {
                updateProperties(nodeType, nodeInstance.getNodeTemplate(), nodeProperties);
            }
        }

        if (nodeCapabilities != null) {
            nodeCapabilities = PatchUtil.realValue(nodeCapabilities);
            if (nodeCapabilities == null) {
                throw new IllegalArgumentException("It is not allowed to set null to capabilities.");
            } else {
                updateCapabilities(nodeInstance.getNodeTemplate(), nodeCapabilities);
            }
        }

        if (nodeAttributeValues != null) {
            nodeAttributeValues = PatchUtil.realValue(nodeAttributeValues);
            if (nodeAttributeValues == null) {
                throw new IllegalArgumentException("It is not allowed to set null to attributes.");
            } else {
                updateAttributes(nodeInstance, nodeAttributeValues);
            }
        }
    }

    private void updateProperties(NodeType nodeType, NodeTemplate nodeTemplate, Map<String, AbstractPropertyValue> nodeProperties)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        for (Map.Entry<String, AbstractPropertyValue> propertyValueEntry : nodeProperties.entrySet()) {
            if (propertyValueEntry.getValue() != null) {
                AbstractPropertyValue value = PatchUtil.realValue(propertyValueEntry.getValue());
                PropertyDefinition propertyDefinition = safe(nodeType.getProperties()).get(propertyValueEntry.getKey());
                if (propertyDefinition == null) {
                    throw new NotFoundException("No property <" + propertyValueEntry.getKey() + "> can be found for node type <" + nodeType.getElementId()
                            + "> in version <" + nodeType.getArchiveVersion() + ">");
                }
                propertyService.setPropertyValue(nodeTemplate, propertyDefinition, propertyValueEntry.getKey(), value);
            }
        }
    }

    private void updateCapabilities(NodeTemplate nodeTemplate, Map<String, Capability> nodeCapabilities)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        for (Map.Entry<String, Capability> entry : nodeCapabilities.entrySet()) {
            if (entry != null) {
                Capability patchCapability = PatchUtil.realValue(entry.getValue());
                if (patchCapability == null) {
                    throw new IllegalArgumentException("It is not allowed to set null to a capability.");
                } else {
                    Capability targetCapability = nodeTemplate.getCapabilities().get(entry.getKey());
                    if (targetCapability == null) {
                        throw new NotFoundException("Capability <" + entry.getKey() + "> doesn't exists on the node.");
                    }
                    CapabilityType capabilityType = ToscaContext.get(CapabilityType.class, targetCapability.getType());
                    updateCapabilitiesProperties(capabilityType, targetCapability, patchCapability);
                }
            }
        }
    }

    private void updateCapabilitiesProperties(CapabilityType capabilityType, Capability targetCapability, Capability patchCapability)
            throws ConstraintValueDoNotMatchPropertyTypeException, ConstraintViolationException {
        for (Map.Entry<String, AbstractPropertyValue> propertyValueEntry : patchCapability.getProperties().entrySet()) {
            if (propertyValueEntry.getValue() != null) {
                AbstractPropertyValue value = PatchUtil.realValue(propertyValueEntry.getValue());
                PropertyDefinition propertyDefinition = safe(capabilityType.getProperties()).get(propertyValueEntry.getKey());
                if (propertyDefinition == null) {
                    throw new NotFoundException(
                            "No property <" + propertyValueEntry.getKey() + "> can be found for capability <" + capabilityType.getElementId() + ">");
                }
                propertyService.setCapabilityPropertyValue(targetCapability, propertyDefinition, propertyValueEntry.getKey(), value);
            }
        }
    }

    private void updateAttributes(NodeInstance nodeInstance, Map<String, String> nodeAttributeValues) {
        for (Map.Entry<String, String> entry : nodeAttributeValues.entrySet()) {
            if (entry.getValue() != null) {
                String value = PatchUtil.realValue(entry.getValue());
                if (value == null) {
                    nodeInstance.getAttributeValues().remove(value);
                }
                nodeInstance.getAttributeValues().put(entry.getKey(), value);
            }
        }
    }

    /**
     * Performs validation of the node instance. Note that based on the actual node state the validation is more or less strict.
     *
     * When the node state is initial the validation checks that all elements defined in the node template or node instance attributes matches the definition of
     * the node
     * type. It however does not check if the type has all required properties configured.
     *
     * When the node state is anything else the validation performs above validation and also checks that all required properties are defined.
     *
     * @param nodeType The node type against which to perform validation of the node instance.
     * @param nodeInstance The actual node instance to validate
     */
    @ToscaContextual
    public void validate(NodeType nodeType, NodeInstance nodeInstance) {
        // FIXME we need a proper template validation here
        // nodeTemplatePostProcessor.process(nodeInstance.getNodeTemplate());

        if (!ToscaNodeLifecycleConstants.INITIAL.equals(safe(nodeInstance.getAttributeValues()).get(ToscaNodeLifecycleConstants.ATT_STATE))) {
            // FIXME check that all required properties are defined.
        }
    }
}
