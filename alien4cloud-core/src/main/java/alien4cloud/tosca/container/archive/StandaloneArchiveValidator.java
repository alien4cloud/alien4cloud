package alien4cloud.tosca.container.archive;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.component.model.IndexedCapabilityType;
import alien4cloud.component.model.IndexedModelUtils;
import alien4cloud.component.model.IndexedNodeType;
import alien4cloud.component.model.IndexedToscaElement;
import alien4cloud.csar.model.Csar;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.model.ToscaElement;
import alien4cloud.tosca.container.model.ToscaInheritableElement;
import alien4cloud.tosca.container.model.type.CapabilityDefinition;
import alien4cloud.tosca.container.model.type.NodeType;
import alien4cloud.tosca.container.model.type.RequirementDefinition;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.tosca.container.validation.CSARErrorFactory;
import alien4cloud.tosca.container.validation.CSARValidationResult;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Bean that validates a {@link CloudServiceArchive} without checking that types exists in it's dependencies. On Alien for cloud we use the
 * {@link ArchiveValidator} that checks in ElasticSearch for referenced types.
 */
@Getter
@Setter
public class StandaloneArchiveValidator implements IArchiveValidator {

    @Resource
    private Validator validator;

    @Override
    public CSARValidationResult validateArchive(CloudServiceArchive cloudServiceARchive) {
        Map<String, Set<CSARError>> allViolations = Maps.newHashMap();
        // meta-data validations
        Set<CSARError> csarMetaErros = validateConstraint(cloudServiceARchive.getMeta());
        if (csarMetaErros != null && !csarMetaErros.isEmpty()) {
            allViolations.put(ArchiveParser.META_FILE_LOCATION, csarMetaErros);
        }
        // definitions validations
        for (Map.Entry<String, Definitions> definitionEntry : cloudServiceARchive.getDefinitions().entrySet()) {
            Set<CSARError> definitionsViolations = validateConstraint(definitionEntry.getValue());
            if (definitionsViolations != null && !definitionsViolations.isEmpty()) {
                allViolations.put(definitionEntry.getKey(), definitionsViolations);
            }
        }
        // Perform validation that super type exists in our repository
        validateSuperTypes(allViolations, cloudServiceARchive);
        // Referenced type must exist also
        validateReferencedTypes(allViolations, cloudServiceARchive);
        Csar csar = null;
        if (cloudServiceARchive.getMeta() != null && cloudServiceARchive.getMeta().getName() != null && cloudServiceARchive.getMeta().getVersion() != null) {
            csar = new Csar();
            csar.setName(cloudServiceARchive.getMeta().getName());
            csar.setVersion(cloudServiceARchive.getMeta().getVersion());
        }
        return new CSARValidationResult(csar, allViolations);
    }

    private <T> Set<CSARError> validateConstraint(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        Set<CSARError> wrappedErrors = Sets.newHashSet();
        for (ConstraintViolation<T> violation : violations) {
            wrappedErrors.add(CSARErrorFactory.createValidationError(violation));
        }
        return wrappedErrors;
    }

    /**
     * Validates that super class exists.
     * 
     * @param allViolations The existing validation errors.
     * @param cloudServiceArchive The archive to validate.
     */
    private void validateSuperTypes(Map<String, Set<CSARError>> allViolations, CloudServiceArchive cloudServiceArchive) {
        for (ToscaElement element : cloudServiceArchive.getArchiveInheritableElements().values()) {
            if (element instanceof ToscaInheritableElement) {
                ToscaInheritableElement inheritableElement = (ToscaInheritableElement) element;
                String superClassType = inheritableElement.getDerivedFrom();
                if (superClassType != null
                        && !isElementExist(IndexedModelUtils.getIndexClass(inheritableElement.getClass()), superClassType, cloudServiceArchive)) {
                    CSARError error = CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.SUPER_TYPE_NOT_FOUND, inheritableElement.getId(), superClassType);
                    addErrorToMap(allViolations, error, cloudServiceArchive, inheritableElement);
                }
            }
        }
    }

    private void addErrorToMap(Map<String, Set<CSARError>> errors, CSARError error, CloudServiceArchive cloudServiceArchive, ToscaElement element) {
        String definitionKey = cloudServiceArchive.getElementDefinition(element.getId());
        Set<CSARError> errorsForDef = errors.get(definitionKey);
        if (errorsForDef == null) {
            errorsForDef = Sets.newHashSet();
            errors.put(definitionKey, errorsForDef);
        }
        errorsForDef.add(error);
    }

    /**
     * Validates that each time a type is referenced, it exists
     * 
     * @param allViolations The existing validation errors.
     * @param cloudServiceArchive The archive to validate.
     */
    private void validateReferencedTypes(Map<String, Set<CSARError>> allViolations, CloudServiceArchive cloudServiceArchive) {
        for (ToscaElement element : cloudServiceArchive.getArchiveInheritableElements().values()) {
            if (element instanceof NodeType) {
                NodeType nodeType = (NodeType) element;
                if (nodeType.getCapabilities() != null) {
                    for (CapabilityDefinition definition : nodeType.getCapabilities().values()) {
                        String definitionType = definition.getType();
                        if (!isElementExist(IndexedCapabilityType.class, definitionType, cloudServiceArchive)) {
                            CSARError error = CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.TYPE_NOT_FOUND, nodeType.getId(), definitionType);
                            addErrorToMap(allViolations, error, cloudServiceArchive, element);
                        }
                    }
                }
                if (nodeType.getRequirements() != null) {
                    for (RequirementDefinition definition : nodeType.getRequirements().values()) {
                        String requirementType = definition.getType();
                        if (!(isElementExist(IndexedCapabilityType.class, requirementType, cloudServiceArchive) || isElementExist(IndexedNodeType.class,
                                requirementType, cloudServiceArchive))) {
                            CSARError error = CSARErrorFactory.createTypeNotFoundError(CSARErrorCode.TYPE_NOT_FOUND, nodeType.getId(), requirementType);
                            addErrorToMap(allViolations, error, cloudServiceArchive, element);
                        }
                    }
                }
            }
        }
    }

    protected boolean isElementExist(Class<? extends IndexedToscaElement> classToSearchFor, String element, CloudServiceArchive cloudServiceArchive) {
        return cloudServiceArchive.getArchiveInheritableElements().containsKey(element);
    }
}