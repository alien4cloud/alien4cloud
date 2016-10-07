package alien4cloud.tosca.parser.postprocess;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.definitions.FunctionPropertyValue;
import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;
import org.alien4cloud.tosca.model.definitions.PropertyDefinition;
import org.alien4cloud.tosca.model.definitions.PropertyValue;
import org.alien4cloud.tosca.model.templates.Topology;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;
import alien4cloud.utils.services.ConstraintPropertyService;

/**
 * Check that property values are matching their definitions.
 */
@Component
public class PropertyValueChecker {
    @Resource
    private ConstraintPropertyService constraintPropertyService;

    /**
     * Check that the value of a property has the right type and match constraints.
     *
     * @param type The type that defines the properties (NodeType, CapabilityType, RequirementType).
     * @param propertyValues The map of values.
     * @param templateName The name of the node template /capability template / requirement template.
     */
    public void checkProperties(final AbstractInheritableToscaType type, final Map<String, AbstractPropertyValue> propertyValues, final String templateName) {
        if (type == null) {
            return; // if the type is null we cannot check properties against their definition. Error is managed elsewhere.
        }
        ArchiveRoot archiveRoot = (ArchiveRoot) ParsingContextExecution.getRoot().getWrappedInstance();
        Topology topology = archiveRoot.getTopology();

        for (Map.Entry<String, AbstractPropertyValue> propertyEntry : safe(propertyValues).entrySet()) {
            String propertyName = propertyEntry.getKey();
            AbstractPropertyValue propertyValue = propertyEntry.getValue();
            Node propertyValueNode = ParsingContextExecution.getObjectToNodeMap().get(propertyValue);
            if (type.getProperties() == null || !type.getProperties().containsKey(propertyName)) {
                ParsingContextExecution.getParsingErrors()
                        .add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.UNRECOGNIZED_PROPERTY, templateName, propertyValueNode.getStartMark(),
                                "Property " + propertyName + " does not exist in type " + type.getElementId(), propertyValueNode.getEndMark(), propertyName));
                continue;
            }
            PropertyDefinition propertyDefinition = type.getProperties().get(propertyName);
            checkProperty(propertyName, propertyValueNode, propertyValue, propertyDefinition, topology.getInputs(), templateName);
        }
    }

    public void checkProperty(String propertyName, Node propertyValueNode, AbstractPropertyValue propertyValue, PropertyDefinition propertyDefinition,
            Map<String, PropertyDefinition> inputs, String templateName) {
        if (propertyValue instanceof FunctionPropertyValue) {
            FunctionPropertyValue function = (FunctionPropertyValue) propertyValue;
            String parameters = function.getParameters().get(0);
            // check get_input only
            if (function.getFunction().equals("get_input")) {
                if (inputs == null || !inputs.keySet().contains(parameters)) {
                    ParsingContextExecution.getParsingErrors().add(new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.MISSING_TOPOLOGY_INPUT, templateName,
                            propertyValueNode.getStartMark(), parameters, propertyValueNode.getEndMark(), propertyName));
                }
            }
        } else if (propertyValue instanceof PropertyValue<?>) {
            checkProperty(propertyName, propertyValueNode, (PropertyValue<?>) propertyValue, propertyDefinition, templateName);
        }
    }

    public void checkProperty(String propertyName, Node propertyValueNode, PropertyValue<?> propertyValue, PropertyDefinition propertyDefinition,
            String templateName) {
        try {
            constraintPropertyService.checkPropertyConstraint(propertyName, propertyValue.getValue(), propertyDefinition,
                    s -> ParsingContextExecution.getParsingErrors()
                            .add(new ParsingError(ErrorCode.VALIDATION_ERROR, "A value is required but was not found for property " + s, null,
                                    "A value is required but was not found for property " + s, null, "constraints")));
        } catch (ConstraintValueDoNotMatchPropertyTypeException | ConstraintViolationException e) {
            StringBuilder problem = new StringBuilder("Validation issue ");
            if (e.getConstraintInformation() != null) {
                problem.append("for " + e.getConstraintInformation().toString());
            }
            problem.append(e.getMessage());
            ParsingError error = null;
            if(propertyValueNode != null) {
                error = new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, templateName, propertyValueNode.getStartMark(), problem.toString(),
                        propertyValueNode.getEndMark(), propertyName);
            } else {
                error = new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, templateName, null, problem.toString(), null, propertyName);
            }
            ParsingContextExecution.getParsingErrors().add(error);
        }
    }
}