package alien4cloud.tosca.parser.impl.advanced;

import java.util.Set;

import javax.annotation.Resource;

import alien4cloud.tosca.normative.AlienCustomTypes;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

import com.google.common.collect.Sets;

@Component
public class PropertyDefinitionChecker implements IChecker<PropertyDefinition> {

    private static final String KEY = "propertyDefinitionChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Override
    public String getName() {
        return KEY;
    }

    @Override
    public void before(ParsingContextExecution context, Node node) {
    }

    @Override
    public void check(PropertyDefinition propertyDefinition, ParsingContextExecution context, Node node) {
        validateType(propertyDefinition, context, node);
        if (propertyDefinition.getConstraints() != null) {
            Set<String> definedConstraints = Sets.newHashSet();
            for (PropertyConstraint constraint : propertyDefinition.getConstraints()) {
                validate(propertyDefinition, constraint, context, node);
                if (!definedConstraints.add(constraint.getClass().getName())) {
                    context.getParsingErrors().add(
                            new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, "ToscaPropertyConstraintDuplicate", node.getStartMark(),
                                    "Constraint duplicated", node.getEndMark(), "constraint"));
                }
            }
        }
        validateDefaultValue(propertyDefinition, context, node);
    }

    private void validateType(PropertyDefinition propertyDefinition, ParsingContextExecution context, Node node) {
        String propertyType = propertyDefinition.getType();
        if (propertyType == null) {
            context.getParsingErrors().add(
                    new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyType", node.getStartMark(), "Property type must be defined", node.getEndMark(),
                            "type"));
        } else if (!ToscaType.isSimple(propertyType)) {
            if (ToscaType.LIST.equals(propertyType) || ToscaType.MAP.equals(propertyType)) {
                PropertyDefinition entrySchema = propertyDefinition.getEntrySchema();
                if (entrySchema == null) {
                    context.getParsingErrors().add(
                            new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyType", node.getStartMark(), "Type " + propertyType
                                    + " must define entry schema", node.getEndMark(), "type"));
                } else {
                    validateType(entrySchema, context, node);
                }
            } else {
                // It's data type
                ArchiveRoot archiveRoot = (ArchiveRoot) context.getRoot().getWrappedInstance();
                if (!archiveRoot.getDataTypes().containsKey(propertyType)) {
                    if (!searchService.isElementExistInDependencies(IndexedDataType.class, propertyType, archiveRoot.getArchive().getDependencies())) {
                        context.getParsingErrors().add(
                                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyType", node.getStartMark(), "Type " + propertyType
                                        + " is not valid for the property definition", node.getEndMark(), "type"));
                    }
                }
            }
        }
    }

    private void validateDefaultValue(PropertyDefinition propertyDefinition, ParsingContextExecution context, Node node) {
        String defaultAsString = propertyDefinition.getDefault();
        if (defaultAsString == null) {
            return;
        }

        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
        if (toscaType != null) {
            Object defaultValue;
            try {
                defaultValue = toscaType.parse(defaultAsString);
            } catch (InvalidPropertyValueException e) {
                addNonCompatiblePropertyTypeError(propertyDefinition, defaultAsString, node, context);
                return;
            }

            if (propertyDefinition.getConstraints() != null && !propertyDefinition.getConstraints().isEmpty()) {
                for (int i = 0; i < propertyDefinition.getConstraints().size(); i++) {
                    PropertyConstraint constraint = propertyDefinition.getConstraints().get(i);
                    try {
                        constraint.validate(defaultValue);
                    } catch (ConstraintViolationException e) {
                        addNonCompatibleConstraintError(constraint, defaultAsString, node, context);
                    }
                }
            }
        } else if (!AlienCustomTypes.checkDefaultIsObject(defaultAsString)) {
            // TODO check if complex object that default is validated
            addNonCompatiblePropertyTypeError(propertyDefinition, defaultAsString, node, context);
        }
    }

    private void addNonCompatiblePropertyTypeError(PropertyDefinition propertyDefinition, String defaultAsString, Node node, ParsingContextExecution context) {
        context.getParsingErrors().add(
                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyDefaultValueType", node.getStartMark(), "Default value " + defaultAsString
                        + " is not valid or is not supported for the property type " + propertyDefinition.getType(), node.getEndMark(), "default"));
    }

    private void addNonCompatibleConstraintError(PropertyConstraint constraint, String defaultAsString, Node node, ParsingContextExecution context) {
        context.getParsingErrors().add(
                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyDefaultValueConstraints", node.getStartMark(), "Default value " + defaultAsString
                        + " is not valid for the constraint " + constraint.getClass().getSimpleName(), node.getEndMark(), "constraints"));
    }

    private void validate(PropertyDefinition propertyDefinition, PropertyConstraint constraint, ParsingContextExecution context, Node keyNode) {
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(propertyDefinition.getType());
        if (toscaType == null) {
            context.getParsingErrors().add(
                    new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.INVALID_CONSTRAINT, "Constraint parsing issue", keyNode.getStartMark(),
                            "Limitation - Constraint cannot be used for type " + propertyDefinition.getType(), keyNode.getEndMark(), "constraint"));
        } else {
            try {
                constraint.initialize(toscaType);
            } catch (ConstraintValueDoNotMatchPropertyTypeException e) {
                context.getParsingErrors().add(
                        new ParsingError(ParsingErrorLevel.ERROR, ErrorCode.VALIDATION_ERROR, "ToscaPropertyConstraint", keyNode.getStartMark(),
                                e.getMessage(), keyNode.getEndMark(), "constraint"));
                return;
            }
        }
    }
}
