package alien4cloud.tosca.parser.impl.advanced;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.component.ICSARRepositorySearchService;
import alien4cloud.model.components.IndexedDataType;
import alien4cloud.model.components.PropertyConstraint;
import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.IChecker;
import alien4cloud.tosca.parser.ParsingContextExecution;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.properties.constraints.exception.ConstraintValueDoNotMatchPropertyTypeException;
import alien4cloud.utils.services.DependencyService;
import alien4cloud.utils.services.DependencyService.ArchiveDependencyContext;

import com.google.common.collect.Sets;

@Component
public class PropertyDefinitionChecker implements IChecker<PropertyDefinition> {

    private static final String KEY = "propertyDefinitionChecker";

    @Resource
    private ICSARRepositorySearchService searchService;

    @Resource
    private DependencyService dependencyService;

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
                    IndexedDataType dataType = dependencyService.getDataType(propertyType, new ArchiveDependencyContext(archiveRoot));
                    if (dataType == null) {
                        context.getParsingErrors().add(
                                new ParsingError(ErrorCode.VALIDATION_ERROR, "ToscaPropertyType", node.getStartMark(), "Type " + propertyType
                                        + " is not valid for the property definition", node.getEndMark(), "type"));
                    }
                }
            }
        }
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
