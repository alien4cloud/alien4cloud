package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import lombok.AllArgsConstructor;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.components.constraints.EqualConstraint;
import alien4cloud.model.components.constraints.GreaterOrEqualConstraint;
import alien4cloud.model.components.constraints.GreaterThanConstraint;
import alien4cloud.model.components.constraints.InRangeConstraint;
import alien4cloud.model.components.constraints.LengthConstraint;
import alien4cloud.model.components.constraints.LessOrEqualConstraint;
import alien4cloud.model.components.constraints.LessThanConstraint;
import alien4cloud.model.components.constraints.MaxLengthConstraint;
import alien4cloud.model.components.constraints.MinLengthConstraint;
import alien4cloud.model.components.constraints.PatternConstraint;
import alien4cloud.model.components.constraints.ValidValuesConstraint;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.AbstractTypeNodeParser;
import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParserUtils;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingTechnicalException;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Parse a constraint based on the specified operator
 */
@Component
public class ConstraintParser extends AbstractTypeNodeParser implements INodeParser<PropertyConstraint> {
    @Resource
    private ScalarParser scalarParser;
    private Map<String, ConstraintParsingInfo> constraintBuildersMap;

    public ConstraintParser() {
        super("Constraints");
    }

    @PostConstruct
    public void init() {
        constraintBuildersMap = Maps.newHashMap();
        constraintBuildersMap.put("equal", new ConstraintParsingInfo(EqualConstraint.class, "equal", scalarParser));
        constraintBuildersMap.put("greater_than", new ConstraintParsingInfo(GreaterThanConstraint.class, "greaterThan", scalarParser));
        constraintBuildersMap.put("greater_or_equal", new ConstraintParsingInfo(GreaterOrEqualConstraint.class, "greaterOrEqual", scalarParser));
        constraintBuildersMap.put("less_than", new ConstraintParsingInfo(LessThanConstraint.class, "lessThan", scalarParser));
        constraintBuildersMap.put("less_or_equal", new ConstraintParsingInfo(LessOrEqualConstraint.class, "lessOrEqual", scalarParser));
        constraintBuildersMap.put("in_range", new ConstraintParsingInfo(InRangeConstraint.class, "inRange", new ListParser<String>(scalarParser,
                "in range constraint expression")));
        constraintBuildersMap.put("valid_values", new ConstraintParsingInfo(ValidValuesConstraint.class, "validValues", new ListParser<String>(scalarParser,
                "valid values constraint expression")));
        constraintBuildersMap.put("length", new ConstraintParsingInfo(LengthConstraint.class, "length", scalarParser));
        constraintBuildersMap.put("min_length", new ConstraintParsingInfo(MinLengthConstraint.class, "minLength", scalarParser));
        constraintBuildersMap.put("max_length", new ConstraintParsingInfo(MaxLengthConstraint.class, "maxLength", scalarParser));
        constraintBuildersMap.put("pattern", new ConstraintParsingInfo(PatternConstraint.class, "pattern", scalarParser));
    }

    @Override
    public boolean isDeferred(ParsingContextExecution context) {
        return true;
    }

    @Override
    public int getDefferedOrder(ParsingContextExecution context) {
        // the deferred parser order will be :
        // [PropertyTypeParser - 1] => [PropertyDefaultValueParser - 2] => [ConstraintParser - 3]
        return 2;
    }

    @Override
    public PropertyConstraint parse(Node node, ParsingContextExecution context) {
        if (node instanceof MappingNode) {
            MappingNode mappingNode = (MappingNode) node;
            if (mappingNode.getValue().size() == 1) {
                NodeTuple nodeTuple = mappingNode.getValue().get(0);
                String operator = ParserUtils.getScalar(nodeTuple.getKeyNode(), context);
                // based on the operator we should load the right constraint.
                return parseConstraint(operator, nodeTuple.getKeyNode(), nodeTuple.getValueNode(), context);
            } else {
                ParserUtils.addTypeError(node, context.getParsingErrors(), "Constraint");
            }
        } else {
            ParserUtils.addTypeError(node, context.getParsingErrors(), "Constraint");
        }
        return null;
    }

    private PropertyConstraint parseConstraint(String operator, Node keyNode, Node expressionNode, ParsingContextExecution context) {
        ConstraintParsingInfo info = constraintBuildersMap.get(operator);
        if (info == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_CONSTRAINT, "Constraint parsing issue", keyNode.getStartMark(),
                            "Unknown constraint operator, will be ignored.", keyNode.getEndMark(), operator));
            return null;
        }
        PropertyConstraint constraint;
        try {
            constraint = info.constraintClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ParsingTechnicalException("Unable to create constraint.", e);
        }
        BeanWrapper target = new BeanWrapperImpl(constraint);
        parseAndSetValue(target, null, expressionNode, context, new MappingTarget(info.expressionPropertyName, info.expressionParser));
        validate(constraint, context, operator, keyNode);
        return constraint;
    }

    private void validate(PropertyConstraint constraint, ParsingContextExecution context, String operator, Node keyNode) {
        PropertyDefinition propertyDefinition = (PropertyDefinition) context.getParent();
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
        if (toscaType == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_CONSTRAINT, "Constraint parsing issue", keyNode.getStartMark(),
                            "Constraint invalid for type " + propertyDefinition.getType(), keyNode.getEndMark(), operator));
        } else {
            try {
                constraint.initialize(toscaType);
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, "ToscaPropertyConstraintValidator", keyNode.getStartMark(),
                                "Constraint value do not match type " + propertyDefinition.getType(), keyNode.getEndMark(), "constraints"));
                return;
            }
            Set<String> definedConstraints = Sets.newHashSet();
            for (int i = 0; i < propertyDefinition.getConstraints().size(); i++) {
                PropertyConstraint existing = propertyDefinition.getConstraints().get(i);
                definedConstraints.add(existing.getClass().getName());
            }
            if (definedConstraints.contains(constraint.getClass().getName())) {

            }
        }
    }

    @AllArgsConstructor
    private class ConstraintParsingInfo {
        private Class<? extends PropertyConstraint> constraintClass;
        private String expressionPropertyName;
        private INodeParser<?> expressionParser;
    }
}