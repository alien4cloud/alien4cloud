package alien4cloud.tosca.parser;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.model.components.*;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.model.topology.RelationshipTemplate;
import alien4cloud.model.topology.Topology;
import alien4cloud.tosca.context.ToscaContext;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.normative.IPropertyType;
import alien4cloud.tosca.normative.InvalidPropertyValueException;
import alien4cloud.tosca.normative.ToscaType;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.tosca.properties.constraints.exception.ConstraintViolationException;

@Component
public class TemplatePostProcessor {



    /**
     * Post process the archive: For every definition of the model it fills the id fields in the TOSCA elements from the key of the elements map.
     *
     * @param parsedArchive The archive to post process
     */
    public ParsingResult<ArchiveRoot> process(ParsingResult<ArchiveRoot> parsedArchive) {
        Map<String, String> globalElementsMap = Maps.newHashMap();
        postProcessArchive(parsedArchive.getResult().getArchive().getName(), parsedArchive.getResult().getArchive().getVersion(), parsedArchive,
                globalElementsMap);
        return parsedArchive;
    }

    private final void postProcessArchive(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, String> globalElementsMap) {
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getNodeTypes(), globalElementsMap);
        postProcessPropertyDefinitions(parsedArchive.getResult().getNodeTypes(), parsedArchive);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getNodeTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getRelationshipTypes(), globalElementsMap);
        postProcessPropertyDefinitions(parsedArchive.getResult().getRelationshipTypes(), parsedArchive);
        postProcessIndexedArtifactToscaElement(parsedArchive.getResult(), parsedArchive.getResult().getRelationshipTypes());
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getCapabilityTypes(), globalElementsMap);
        postProcessPropertyDefinitions(parsedArchive.getResult().getCapabilityTypes(), parsedArchive);
        postProcessElements(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getArtifactTypes(), globalElementsMap);
        postProcessTopology(archiveName, archiveVersion, parsedArchive, parsedArchive.getResult().getTopology(), globalElementsMap);
    }

    private final <T extends IndexedInheritableToscaElement> void postProcessPropertyDefinitions(Map<String, T> elements,
            ParsingResult<ArchiveRoot> parsedArchive) {
        for (Entry<String, T> elementEntry : elements.entrySet()) {
            Map<String, PropertyDefinition> propertyDefinitions = elementEntry.getValue().getProperties();
            if (propertyDefinitions != null) {
                for (Entry<String, PropertyDefinition> propertyDefinitionEntry : propertyDefinitions.entrySet()) {
                    String propertyFqn = elementEntry.getKey() + "." + propertyDefinitionEntry.getKey();
                    validateDefaultValue(propertyDefinitionEntry.getValue(), propertyFqn, parsedArchive);
                }
            }
        }
    }

    private void validateDefaultValue(PropertyDefinition propertyDefinition, String propertyFqn, ParsingResult<ArchiveRoot> parsedArchive) {
        // TODO:XDE
        AbstractPropertyValue defaultObject = propertyDefinition.getDefault();
        if (defaultObject == null) {
            return;
        }
        //
        if (ToscaType.isSimple(propertyDefinition.getType())) {
            if (!(defaultObject instanceof ScalarPropertyValue)) {
                addNonCompatiblePropertyTypeError(propertyDefinition.getType(), defaultObject.toString(), propertyFqn, parsedArchive);
                return;
            }
            ScalarPropertyValue scalarPropertyValue = (ScalarPropertyValue) defaultObject;
            String defaultAsString = scalarPropertyValue.getValue();
            validateSimpleObjectValue(propertyDefinition.getType(), propertyDefinition.getConstraints(), defaultAsString, propertyFqn, parsedArchive);
        } else if (ToscaType.LIST.equals(propertyDefinition.getType())) {
            if (!(defaultObject instanceof ListPropertyValue)) {
                addNonCompatiblePropertyTypeError(propertyDefinition.getType(), defaultObject.toString(), propertyFqn, parsedArchive);
                return;
            }
            ListPropertyValue listPropertyValue = (ListPropertyValue) defaultObject;
            PropertyDefinition entryPropertyDefinition = propertyDefinition.getEntrySchema();
            List<Object> entries = listPropertyValue.getValue();
            int i = 0;
            for (Object value : entries) {
                validateObjectValue(entryPropertyDefinition, value, propertyFqn + "[" + ++i + "]", parsedArchive);
            }
        } else if (ToscaType.MAP.equals(propertyDefinition.getType())) {
            if (!(defaultObject instanceof ComplexPropertyValue)) {
                addNonCompatiblePropertyTypeError(propertyDefinition.getType(), defaultObject.toString(), propertyFqn, parsedArchive);
                return;
            }
            ComplexPropertyValue complexPropertyValue = (ComplexPropertyValue) defaultObject;
            Map<String, Object> complexPropertyValueMap = complexPropertyValue.getValue();
            PropertyDefinition entryPropertyDefinition = propertyDefinition.getEntrySchema();
            for (Entry<String, Object> entry : complexPropertyValueMap.entrySet()) {
                validateObjectValue(entryPropertyDefinition, entry.getValue(), propertyFqn + "." + entry.getKey(), parsedArchive);
            }
        } else if (!ToscaType.isPrimitive(propertyDefinition.getType())) {
            // complex object
            // complex type that derives from simple
            IndexedDataType dataType = ToscaContext.get(IndexedDataType.class, propertyDefinition.getType());
            if (dataType instanceof PrimitiveIndexedDataType) {
                if (!(defaultObject instanceof ScalarPropertyValue)) {
                    addNonCompatiblePropertyTypeError(propertyDefinition.getType(), defaultObject.toString(), propertyFqn, parsedArchive);
                    return;
                }
                ScalarPropertyValue scalarPropertyValue = (ScalarPropertyValue) defaultObject;
                String defaultAsString = scalarPropertyValue.getValue();
                List<PropertyConstraint> constraints = Lists.newArrayList();
                if (propertyDefinition.getConstraints() != null) {
                    constraints.addAll(propertyDefinition.getConstraints());
                }
                if (((PrimitiveIndexedDataType) dataType).getConstraints() != null) {
                    constraints.addAll(((PrimitiveIndexedDataType) dataType).getConstraints());
                }
                validateSimpleObjectValue(dataType.getDerivedFrom().get(0), constraints, defaultAsString, propertyFqn, parsedArchive);
            } else {
                if (!(defaultObject instanceof ComplexPropertyValue)) {
                    addNonCompatiblePropertyTypeError(propertyDefinition.getType(), defaultObject.toString(), propertyFqn, parsedArchive);
                    return;
                }
                ComplexPropertyValue complexPropertyValue = (ComplexPropertyValue) defaultObject;
                Map<String, Object> complexPropertyValueMap = complexPropertyValue.getValue();
                for (Entry<String, PropertyDefinition> propEntry : dataType.getProperties().entrySet()) {
                    Object propertyValue = complexPropertyValueMap.get(propEntry.getKey());
                    PropertyDefinition propertyPropertyDefinition = propEntry.getValue();
                    validateObjectValue(propertyPropertyDefinition, propertyValue, propertyFqn + "." + propEntry.getKey(), parsedArchive);
                }
            }

        }
    }

    private void validateObjectValue(PropertyDefinition propertyDefinition, Object value, String propertyFqn, ParsingResult<ArchiveRoot> parsedArchive) {
        if (value == null) {
            if (propertyDefinition.isRequired()) {
                parsedArchive.getContext().getParsingErrors()
                        .add(new ParsingError(ErrorCode.VALIDATION_ERROR, "A value is required but was not found for property " + propertyFqn, null,
                                "A value is required but was not found for property " + propertyFqn, null, "constraints"));
            }
            return;
        }
        if (ToscaType.isSimple(propertyDefinition.getType())) {
            validateSimpleObjectValue(propertyDefinition.getType(), propertyDefinition.getConstraints(), value.toString(), propertyFqn, parsedArchive);
        } else if (ToscaType.LIST.equals(propertyDefinition.getType())) {
            if (!(value instanceof List)) {
                addNonCompatiblePropertyTypeError(propertyDefinition.getType(), value.toString(), propertyFqn, parsedArchive);
                return;
            }
            PropertyDefinition entryPropertyDefinition = propertyDefinition.getEntrySchema();
            List<Object> entries = (List<Object>) value;
            int i = 0;
            for (Object entry : entries) {
                validateObjectValue(entryPropertyDefinition, entry, propertyFqn + "[" + ++i + "]", parsedArchive);
            }
        } else if (ToscaType.MAP.equals(propertyDefinition.getType())) {
            if (!(value instanceof Map)) {
                addNonCompatiblePropertyTypeError(propertyDefinition.getType(), value.toString(), propertyFqn, parsedArchive);
                return;
            }
            PropertyDefinition entryPropertyDefinition = propertyDefinition.getEntrySchema();
            Map<String, Object> valueMap = (Map<String, Object>) value;
            for (Entry<String, Object> entry : valueMap.entrySet()) {
                validateObjectValue(entryPropertyDefinition, entry.getValue(), propertyFqn + "." + entry.getKey(), parsedArchive);
            }
        } else if (!ToscaType.isPrimitive(propertyDefinition.getType())) {
            // complex object
            IndexedDataType dataType = ToscaContext.get(IndexedDataType.class, propertyDefinition.getType());
            if (dataType instanceof PrimitiveIndexedDataType) {
                List<PropertyConstraint> constraints = Lists.newArrayList();
                if (propertyDefinition.getConstraints() != null) {
                    constraints.addAll(propertyDefinition.getConstraints());
                }
                if (((PrimitiveIndexedDataType) dataType).getConstraints() != null) {
                    constraints.addAll(((PrimitiveIndexedDataType) dataType).getConstraints());
                }
                validateSimpleObjectValue(dataType.getDerivedFrom().get(0), constraints, value.toString(), propertyFqn, parsedArchive);

            } else {
                if (!(value instanceof Map)) {
                    addNonCompatiblePropertyTypeError(propertyDefinition.getType(), value.toString(), propertyFqn, parsedArchive);
                    return;
                }
                Map<String, Object> complexPropertyValueMap = (Map<String, Object>) value;
                for (Entry<String, PropertyDefinition> propEntry : dataType.getProperties().entrySet()) {
                    Object propertyValue = complexPropertyValueMap.get(propEntry.getKey());
                    PropertyDefinition propertyPropertyDefinition = propEntry.getValue();
                    validateObjectValue(propertyPropertyDefinition, propertyValue, propertyFqn + "." + propEntry.getKey(), parsedArchive);
                }
            }
        }
    }

    private void addNonCompatiblePropertyTypeError(String type, String defaultAsString, String propertyFqn, ParsingResult<ArchiveRoot> parsedArchive) {
        parsedArchive.getContext().getParsingErrors()
                .add(new ParsingError(ErrorCode.VALIDATION_ERROR,
                        "Default value " + defaultAsString + " is not valid or is not supported for the property type " + type + " ( + " + propertyFqn + ")",
                        null, "Default value " + defaultAsString + " is not valid or is not supported for the property type " + type, null, "default"));
    }

    private void addNonCompatibleConstraintError(PropertyConstraint constraint, String defaultAsString, String propertyFqn,
            ParsingResult<ArchiveRoot> parsedArchive) {
        parsedArchive.getContext().getParsingErrors()
                .add(new ParsingError(ErrorCode.VALIDATION_ERROR,
                        "Default value " + defaultAsString + " is not valid regarding the constraint " + constraint.getClass().getSimpleName()
                                + " for property " + propertyFqn,
                        null, "Default value " + defaultAsString + " is not valid for the constraint " + constraint.getClass().getSimpleName(), null,
                        "constraints"));
    }

    private void validateSimpleObjectValue(String type, List<PropertyConstraint> constraints, String value, String propertyFqn,
            ParsingResult<ArchiveRoot> parsedArchive) {
        IPropertyType<?> toscaType = ToscaType.fromYamlTypeName(type);
        Object defaultValue;
        try {
            defaultValue = toscaType.parse(value);
        } catch (InvalidPropertyValueException e) {
            addNonCompatiblePropertyTypeError(type, value, propertyFqn, parsedArchive);
            return;
        }
        if (constraints != null && !constraints.isEmpty()) {
            for (int i = 0; i < constraints.size(); i++) {
                PropertyConstraint constraint = constraints.get(i);
                try {
                    constraint.validate(defaultValue);
                } catch (ConstraintViolationException e) {
                    addNonCompatibleConstraintError(constraint, value, propertyFqn, parsedArchive);
                }
            }
        }
    }

    private void postProcessTopology(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, Topology topology,
            Map<String, String> globalElementsMap) {
        if (topology == null) {
            return;
        }
        for (NodeTemplate nodeTemplate : topology.getNodeTemplates().values()) {
            postProcessNodeTemplate(archiveName, archiveVersion, parsedArchive, nodeTemplate, globalElementsMap);
        }
    }

    private void postProcessNodeTemplate(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive, NodeTemplate nodeTemplate,
            Map<String, String> globalElementsMap) {
        postProcessInterfaces(parsedArchive.getResult(), nodeTemplate.getInterfaces());
        if (nodeTemplate.getRelationships() != null) {
            for (RelationshipTemplate relationship : nodeTemplate.getRelationships().values()) {
                postProcessInterfaces(parsedArchive.getResult(), relationship.getInterfaces());
            }
        }
    }

    private final void postProcessElements(String archiveName, String archiveVersion, ParsingResult<ArchiveRoot> parsedArchive,
            Map<String, ? extends IndexedInheritableToscaElement> elements, Map<String, String> globalElementsMap) {
        if (elements == null) {
            return;
        }
        for (Map.Entry<String, ? extends IndexedInheritableToscaElement> element : elements.entrySet()) {
            element.getValue().setId(element.getKey());
            element.getValue().setArchiveName(archiveName);
            element.getValue().setArchiveVersion(archiveVersion);
            String previous = globalElementsMap.put(element.getKey(), parsedArchive.getContext().getFileName());
            if (previous != null) {
                parsedArchive.getContext().getParsingErrors().add(new ParsingError(ErrorCode.DUPLICATED_ELEMENT_DECLARATION,
                        "Type is defined twice in archive.", null, parsedArchive.getContext().getFileName(), null, previous));
            }
        }
    }

    private void postProcessIndexedArtifactToscaElement(ArchiveRoot archive, Map<String, ? extends IndexedArtifactToscaElement> elements) {
        if (elements == null) {
            return;
        }
        for (IndexedArtifactToscaElement element : elements.values()) {
            postProcessDeploymentArtifacts(archive, element);
            postProcessInterfaces(archive, element.getInterfaces());
        }
    }

    private void postProcessDeploymentArtifacts(ArchiveRoot archive, IndexedArtifactToscaElement element) {
        if (element.getArtifacts() == null) {
            return;
        }

        for (DeploymentArtifact artifact : element.getArtifacts().values()) {
            postProcessDeploymentArtifact(archive, artifact);
        }
    }

    private void postProcessInterfaces(ArchiveRoot archive, Map<String, Interface> interfaces) {
        if (interfaces == null) {
            return;
        }

        for (Interface interfaz : interfaces.values()) {
            for (Operation operation : interfaz.getOperations().values()) {
                postProcessImplementationArtifact(archive, operation.getImplementationArtifact());
            }
        }
    }

    private void postProcessDeploymentArtifact(ArchiveRoot archive, DeploymentArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }

    private void postProcessImplementationArtifact(ArchiveRoot archive, ImplementationArtifact artifact) {
        if (artifact != null) {
            artifact.setArchiveName(archive.getArchive().getName());
            artifact.setArchiveVersion(archive.getArchive().getVersion());
        }
    }
}
