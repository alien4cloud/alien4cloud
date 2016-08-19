package alien4cloud.ui.form;

import static alien4cloud.ui.form.GenericFormConstants.ABSTRACT_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.ARRAY_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.BOOLEAN_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.COMPLEX_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.CONTENT_TYPE_KEY;
import static alien4cloud.ui.form.GenericFormConstants.DATE_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.IMPLEMENTATIONS_KEY;
import static alien4cloud.ui.form.GenericFormConstants.IS_PASSWORD_KEY;
import static alien4cloud.ui.form.GenericFormConstants.LABEL_KEY;
import static alien4cloud.ui.form.GenericFormConstants.MAP_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.NOT_NULL_KEY;
import static alien4cloud.ui.form.GenericFormConstants.NUMBER_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.ORDER_KEY;
import static alien4cloud.ui.form.GenericFormConstants.PROPERTY_TYPE_KEY;
import static alien4cloud.ui.form.GenericFormConstants.STRING_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.SUGGESTION_KEY;
import static alien4cloud.ui.form.GenericFormConstants.TOSCA_DEFINITION_KEY;
import static alien4cloud.ui.form.GenericFormConstants.TOSCA_TYPE;
import static alien4cloud.ui.form.GenericFormConstants.TYPE_KEY;
import static alien4cloud.ui.form.GenericFormConstants.VALID_VALUES_KEY;

import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormContentTypes;
import alien4cloud.ui.form.annotation.FormCustomType;
import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormPassword;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormPropertyDefinition;
import alien4cloud.ui.form.annotation.FormSuggestion;
import alien4cloud.ui.form.annotation.FormType;
import alien4cloud.ui.form.annotation.FormTypes;
import alien4cloud.ui.form.annotation.FormValidValues;
import alien4cloud.ui.form.exception.FormDescriptorGenerationException;
import alien4cloud.utils.ReflectionUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PojoFormDescriptorGenerator {

    @Resource
    @Setter
    private FormSuggestionDescriptorGenerator suggestionDescriptorGenerator;

    @Resource
    @Setter
    private PropertyDefinitionConverter propertyDefinitionConverter;

    public Map<String, Object> generateDescriptor(Class<?> clazz) {
        return buildComplexTypeDescriptor(clazz);
    }

    private Map<String, Object> buildComplexTypeDescriptor(Class<?> clazz) {
        Map<String, Object> type = Maps.newHashMap();
        type.put(TYPE_KEY, COMPLEX_TYPE);
        doParseClass(clazz, type);
        return type;
    }

    private boolean isPrimitive(Class<?> clazz) {
        return (clazz.isPrimitive() || Number.class.isAssignableFrom(clazz) || Boolean.class == clazz || String.class == clazz
                || Date.class.isAssignableFrom(clazz));
    }

    private Map<String, Object> buildSimpleTypeDescriptor(Class<?> clazz) {
        Map<String, Object> type = Maps.newHashMap();
        String typeName;
        if (clazz.isPrimitive()) {
            if (char.class == clazz) {
                throw new FormDescriptorGenerationException("Cannot handle 'char' type");
            } else if (boolean.class == clazz) {
                typeName = BOOLEAN_TYPE;
            } else {
                typeName = NUMBER_TYPE;
            }
        } else if (Number.class.isAssignableFrom(clazz)) {
            typeName = NUMBER_TYPE;
        } else if (Boolean.class == clazz) {
            typeName = BOOLEAN_TYPE;
        } else if (Character.class == clazz) {
            throw new FormDescriptorGenerationException("Cannot handle 'Character' type");
        } else if (String.class == clazz) {
            typeName = STRING_TYPE;
        } else if (Date.class.isAssignableFrom(clazz)) {
            typeName = DATE_TYPE;
        } else {
            throw new FormDescriptorGenerationException("Unmanaged primitive type [" + clazz.getName() + "]");
        }
        type.put(TYPE_KEY, typeName);
        return type;
    }

    private Map<String, Object> buildSequenceTypeDescriptor(String sequenceTypeName, Class<?> contentType) {
        Map<String, Object> type = Maps.newHashMap();
        type.put(TYPE_KEY, sequenceTypeName);
        if (isPrimitive(contentType)) {
            type.put(CONTENT_TYPE_KEY, buildSimpleTypeDescriptor(contentType));
        } else if (Map.class.isAssignableFrom(contentType)) {
            throw new FormDescriptorGenerationException(
                    "Cannot generate meta data for field of type set of set, set of array, array of array, map of set ... ");
        } else if (List.class.isAssignableFrom(contentType) || Set.class.isAssignableFrom(contentType)) {
            throw new FormDescriptorGenerationException(
                    "Cannot generate meta data for field of type set of set, set of array, array of array, map of set ... ");
        } else if (contentType.isArray()) {
            throw new FormDescriptorGenerationException(
                    "Cannot generate meta data for field of type set of set, set of array, array of array, map of set ... ");
        } else {
            type.put(CONTENT_TYPE_KEY, buildComplexTypeDescriptor(contentType));
        }
        return type;
    }

    private Map<String, Object> buildSequenceAbstractTypeDescriptor(String sequenceTypeName, Map<String, FormType> implementations) {
        Map<String, Object> type = Maps.newHashMap();
        type.put(TYPE_KEY, sequenceTypeName);
        type.put(CONTENT_TYPE_KEY, buildAbstractTypeDescriptor(implementations));
        return type;
    }

    private Map<String, Object> buildAbstractTypeDescriptor(Map<String, FormType> implementations) {
        // Implementations are given
        Map<String, Object> type = Maps.newHashMap();
        type.put(TYPE_KEY, ABSTRACT_TYPE);
        Map<String, Object> implementationDescriptors = Maps.newHashMap();
        type.put(IMPLEMENTATIONS_KEY, implementationDescriptors);
        for (Map.Entry<String, FormType> implEntry : implementations.entrySet()) {
            Map<String, Object> implementationDescriptor = Maps.newHashMap();
            implementationDescriptors.put(implEntry.getKey(), implementationDescriptor);
            implementationDescriptor.put(LABEL_KEY, implEntry.getValue().label());
            doParseClass(implEntry.getValue().implementation(), implementationDescriptor);
        }
        return type;
    }

    private void doParseClass(Class<?> clazz, Map<String, Object> descriptors) {
        Map<String, Object> propertyTypes = Maps.newHashMap();
        descriptors.put(PROPERTY_TYPE_KEY, propertyTypes);
        PropertyDescriptor[] properties = ReflectionUtil.getPropertyDescriptors(clazz);
        String[] orderedPropertiesNames;
        Set<String> propertiesNames;
        FormProperties formPropAnnotation = clazz.getAnnotation(FormProperties.class);
        if (formPropAnnotation != null && formPropAnnotation.value() != null) {
            orderedPropertiesNames = formPropAnnotation.value();
            propertiesNames = Sets.newHashSet(orderedPropertiesNames);
        } else {
            propertiesNames = Sets.newLinkedHashSet();
            for (PropertyDescriptor property : properties) {
                if (property.getReadMethod() != null && property.getWriteMethod() != null) {
                    propertiesNames.add(property.getName());
                }
            }
            orderedPropertiesNames = propertiesNames.toArray(new String[propertiesNames.size()]);
            if (log.isDebugEnabled()) {
                log.debug("Class " + clazz.getName() + " do not have FormProperties annotation, all properties will be read and order will not be assured");
            }
        }
        for (PropertyDescriptor property : properties) {
            if ((!propertiesNames.contains(property.getName())) || property.getReadMethod() == null || property.getWriteMethod() == null) {
                continue;
            }
            Class<?> propClazz = property.getPropertyType();

            Map<String, FormType> implementations = getImplementations(clazz, property);

            Map<String, FormType> contentImplementations = getContentImplementations(clazz, property);

            PropertyDefinition propertyDefinition = getPropertyDefinition(clazz, property);

            String customFormType = getCustomFormType(clazz, property);

            String[] validValues = getValidValues(clazz, property);

            String label = getLabel(clazz, property);

            Map<String, Object> type = Maps.newHashMap();
            if (customFormType != null) {
                // Custom type that cannot be processed in a generic way on ui side
                type.put(TYPE_KEY, customFormType);
            } else if (propertyDefinition != null) {
                type.put(TYPE_KEY, TOSCA_TYPE);
                type.put(TOSCA_DEFINITION_KEY, propertyDefinition);
            } else if (isPrimitive(propClazz)) {
                // Primitive type
                type = buildSimpleTypeDescriptor(propClazz);
                if (validValues != null && validValues.length > 0) {
                    type.put(VALID_VALUES_KEY, validValues);
                }
            } else if (Map.class.isAssignableFrom(propClazz)) {
                // Map type
                if (contentImplementations != null && !contentImplementations.isEmpty()) {
                    type = buildSequenceAbstractTypeDescriptor(MAP_TYPE, contentImplementations);
                } else {
                    ParameterizedType parameterizedType = (ParameterizedType) property.getReadMethod().getGenericReturnType();
                    Type[] types = parameterizedType.getActualTypeArguments();
                    Class<?> keyClass = (Class<?>) types[0];
                    Class<?> valueClass = (Class<?>) types[1];
                    if (keyClass != String.class) {
                        throw new FormDescriptorGenerationException("Cannot generate meta data for map with key not of type String");
                    }
                    type = buildSequenceTypeDescriptor(MAP_TYPE, valueClass);
                }
            } else if (List.class.isAssignableFrom(propClazz) || Set.class.isAssignableFrom(propClazz)) {
                // List or set types
                if (contentImplementations != null && !contentImplementations.isEmpty()) {
                    type = buildSequenceAbstractTypeDescriptor(ARRAY_TYPE, contentImplementations);
                } else {
                    ParameterizedType pt = (ParameterizedType) property.getReadMethod().getGenericReturnType();
                    Type[] types = pt.getActualTypeArguments();
                    Class<?> valueClass = (Class<?>) types[0];
                    type = buildSequenceTypeDescriptor(ARRAY_TYPE, valueClass);
                }
            } else if (propClazz.isArray()) {
                // Array type
                if (contentImplementations != null && !contentImplementations.isEmpty()) {
                    type = buildSequenceAbstractTypeDescriptor(ARRAY_TYPE, contentImplementations);
                } else {
                    Class<?> valueClass = property.getReadMethod().getReturnType().getComponentType();
                    type = buildSequenceTypeDescriptor(ARRAY_TYPE, valueClass);
                }
            } else if (implementations != null && !implementations.isEmpty()) {
                type = buildAbstractTypeDescriptor(implementations);
            } else {
                // Complex type
                Class<?> valueClass = property.getReadMethod().getReturnType();
                type = buildComplexTypeDescriptor(valueClass);
            }
            Map<String, Object> suggestion = getSuggestion(clazz, property);
            if (suggestion != null) {
                type.put(SUGGESTION_KEY, suggestion);
            }
            if (isNotNull(clazz, property)) {
                type.put(NOT_NULL_KEY, true);
            }
            if (isPassword(clazz, property)) {
                type.put(IS_PASSWORD_KEY, true);
            }
            if (label != null) {
                type.put(LABEL_KEY, label);
            }
            propertyTypes.put(property.getName(), type);
        }
        descriptors.put(TYPE_KEY, COMPLEX_TYPE);
        descriptors.put(ORDER_KEY, orderedPropertiesNames);
    }

    private PropertyDefinition getPropertyDefinition(Class<?> clazz, PropertyDescriptor property) {
        FormPropertyDefinition propertyDefinition = ReflectionUtil.getAnnotation(clazz, FormPropertyDefinition.class, property);
        if (propertyDefinition != null) {
            return propertyDefinitionConverter.convert(propertyDefinition);
        } else {
            return null;
        }
    }

    private String[] getValidValues(Class<?> clazz, PropertyDescriptor property) {
        FormValidValues validValues = ReflectionUtil.getAnnotation(clazz, FormValidValues.class, property);
        if (validValues == null) {
            return null;
        } else {
            return validValues.value();
        }
    }

    private Map<String, FormType> getImplementations(Class<?> clazz, PropertyDescriptor property) {
        FormTypes formTypes = ReflectionUtil.getAnnotation(clazz, FormTypes.class, property);
        if (formTypes == null) {
            return null;
        }
        return getImplementations(formTypes.value());
    }

    private Map<String, FormType> getContentImplementations(Class<?> clazz, PropertyDescriptor property) {
        FormContentTypes formTypes = ReflectionUtil.getAnnotation(clazz, FormContentTypes.class, property);
        if (formTypes == null) {
            return null;
        }
        return getImplementations(formTypes.value());
    }

    private Map<String, FormType> getImplementations(FormType[] formTypeValues) {
        if (formTypeValues == null || formTypeValues.length == 0) {
            return null;
        }
        Map<String, FormType> implementations = Maps.newHashMap();
        for (FormType formType : formTypeValues) {
            implementations.put(formType.discriminantProperty(), formType);
        }
        return implementations;
    }

    private String getCustomFormType(Class<?> clazz, PropertyDescriptor property) {
        FormCustomType formCustomType = ReflectionUtil.getAnnotation(clazz, FormCustomType.class, property);
        return formCustomType != null ? formCustomType.value() : null;
    }

    private Map<String, Object> getSuggestion(Class<?> clazz, PropertyDescriptor property) {
        FormSuggestion formSuggestion = ReflectionUtil.getAnnotation(clazz, FormSuggestion.class, property);
        if (formSuggestion != null) {
            return suggestionDescriptorGenerator.generateSuggestionDescriptor(formSuggestion.fromClass(), formSuggestion.path());
        } else {
            return null;
        }
    }

    private String getLabel(Class<?> clazz, PropertyDescriptor property) {
        FormLabel formLabel = ReflectionUtil.getAnnotation(clazz, FormLabel.class, property);
        if (formLabel != null) {
            return formLabel.value();
        } else {
            return null;
        }
    }

    private boolean isNotNull(Class<?> clazz, PropertyDescriptor property) {
        return ReflectionUtil.getAnnotation(clazz, NotNull.class, property) != null;
    }

    private boolean isPassword(Class<?> clazz, PropertyDescriptor property) {
        return ReflectionUtil.getAnnotation(clazz, FormPassword.class, property) != null;
    }
}
