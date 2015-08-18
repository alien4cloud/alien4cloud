package alien4cloud.tosca.parser.impl.advanced;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.mapping.DefaultDeferredParser;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;

@Component
public class PropertyDefaultValueParser extends DefaultDeferredParser<String> {

    @Resource
    private ScalarParser scalarParser;

    private void addNonCompatiblePropertyTypeError(PropertyDefinition propertyDefinition, String defaultAsString, Node node, ParsingContextExecution context) {
        context.getParsingErrors().add(
                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyDefaultValueType", node.getStartMark(), "Default value " + defaultAsString
                        + " is not valid for the property type " + propertyDefinition.getType(), node.getEndMark(), "default"));
    }

    private void addNonCompatibleConstraintError(PropertyConstraint constraint, String defaultAsString, Node node, ParsingContextExecution context) {
        context.getParsingErrors().add(
                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyDefaultValueConstraints", node.getStartMark(), "Default value " + defaultAsString
                        + " is not valid for the constraint " + constraint.getClass().getSimpleName(), node.getEndMark(), "constraints"));
    }

    @Override
    public String parse(Node node, ParsingContextExecution context) {
        String defaultAsString = scalarParser.parse(node, context);
        if (defaultAsString == null) {
            return null;
        }
        PropertyDefinition propertyDefinition = (PropertyDefinition) context.getParent();
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
        if (toscaType != null) {
            try {
                toscaType.parse(defaultAsString);
            } catch (InvalidPropertyValueException e) {
                addNonCompatiblePropertyTypeError(propertyDefinition, defaultAsString, node, context);
            }

            if (propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty()) {
                for (int i = 0; i < propertyDefinition.getConstraints().size(); i++) {
                    PropertyConstraint constraint = propertyDefinition.getConstraints().get(i);
                    try {
                        constraint.initialize(toscaType);
                    } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                        addNonCompatibleConstraintError(constraint, defaultAsString, node, context);
                    }
                }
            }
        } else {
            addNonCompatiblePropertyTypeError(propertyDefinition, defaultAsString, node, context);
        }
        return defaultAsString;
    }

    @Override
    public int getDeferredOrder(ParsingContextExecution context) {
        return 2;
    }
}
