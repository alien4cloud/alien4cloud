package alien4cloud.tosca.parser.impl.advanced;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import alien4cloud.tosca.parser.impl.base.BaseParserFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

import com.google.common.collect.Maps;

import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.constraints.*;
import alien4cloud.tosca.parser.*;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.parser.impl.base.ListParser;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import lombok.AllArgsConstructor;

/**
 * Parse a constraint based on the specified operator
 */
@Component
public class ConstraintParser extends AbstractTypeNodeParser implements INodeParser<PropertyConstraint> {
    @Resource
    private ScalarParser scalarParser;
    @Resource
    private BaseParserFactory baseParserFactory;
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
        constraintBuildersMap.put("in_range",
                new ConstraintParsingInfo(InRangeConstraint.class, "inRange", baseParserFactory.getListParser(scalarParser, "in range constraint expression")));
        constraintBuildersMap.put("valid_values", new ConstraintParsingInfo(ValidValuesConstraint.class, "validValues",
                baseParserFactory.getListParser(scalarParser, "valid values constraint expression")));
        constraintBuildersMap.put("length", new ConstraintParsingInfo(LengthConstraint.class, "length", scalarParser));
        constraintBuildersMap.put("min_length", new ConstraintParsingInfo(MinLengthConstraint.class, "minLength", scalarParser));
        constraintBuildersMap.put("max_length", new ConstraintParsingInfo(MaxLengthConstraint.class, "maxLength", scalarParser));
        constraintBuildersMap.put("pattern", new ConstraintParsingInfo(PatternConstraint.class, "pattern", scalarParser));
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
            context.getParsingErrors().add(new ParsingError(ParsingErrorLevel.WARNING, ErrorCode.UNKNOWN_CONSTRAINT, "Constraint parsing issue",
                    keyNode.getStartMark(), "Unknown constraint operator, will be ignored.", keyNode.getEndMark(), operator));
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
        return constraint;
    }

    @AllArgsConstructor(suppressConstructorProperties = true)
    private class ConstraintParsingInfo {
        private Class<? extends PropertyConstraint> constraintClass;
        private String expressionPropertyName;
        private INodeParser<?> expressionParser;
    }
}