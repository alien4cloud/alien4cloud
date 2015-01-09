package alien4cloud.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

import alien4cloud.exception.InvalidArgumentException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Utility class to help working with java reflection.
 */
@Slf4j
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    /**
     * Get all the interfaces implemented by a given class including interfaces implemented by the parent classes.
     * 
     * @param clazz The actual class for which to get all implemented interfaces.
     */
    public static Set<Type> getAllGenericInterfaces(Class<?> clazz) {
        Set<Type> genericInterfaces = Sets.newHashSet();

        while (clazz != null) {
            Type[] clazzGenericInterfaces = clazz.getGenericInterfaces();

            for (Type clazzGI : clazzGenericInterfaces) {
                if (clazzGI instanceof Class) {
                    genericInterfaces.addAll(getAllGenericInterfaces((Class<?>) clazzGI));
                }
            }

            genericInterfaces.addAll(Arrays.asList(clazzGenericInterfaces));
            clazz = clazz.getSuperclass();
        }

        return genericInterfaces;
    }

    /**
     * Get the Class of the generic argument of a generic class based on it's implementation class.
     * 
     * @param implementationClass The implementation class that actually is a sub-class of generic class.
     * @param genericClass The generic class for which to get the argument type.
     * @param index The index of the argument.
     * @return null if the type cannot be found, the generic type when found.
     */
    public static Class<?> getGenericArgumentType(Class<?> implementationClass, Class<?> genericClass, int index) {
        if (implementationClass.isAssignableFrom(genericClass)) {
            return null;
        }

        if (genericClass.isInterface()) {
            Type generic = getInterfaceTypeArgument(implementationClass, genericClass, index);
            if (generic instanceof TypeVariable) {
                return getTypeVariable(implementationClass.getGenericSuperclass(), (TypeVariable<?>) generic);
            }

            return (Class<?>) generic;
        }

        return null;
    }

    private static Type getInterfaceTypeArgument(Class<?> implementationClass, Class<?> genericClass, int index) {
        Set<Type> genericInterfaces = ReflectionUtil.getAllGenericInterfaces(implementationClass);
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType) {
                ParameterizedType paramGenericInterface = (ParameterizedType) genericInterface;
                if (genericClass.equals(paramGenericInterface.getRawType())) {
                    return paramGenericInterface.getActualTypeArguments()[index];
                }
            }
        }
        return null;
    }

    private static Class<?> getTypeVariable(Type from, TypeVariable<?> typeVariable) {
        Class<?> fromClass;
        if (from instanceof ParameterizedType) {
            ParameterizedType fromParam = (ParameterizedType) from;
            fromClass = (Class<?>) fromParam.getRawType();
            if (fromClass.equals(typeVariable.getGenericDeclaration())) {
                TypeVariable<?>[] fromTypeVariables = fromClass.getTypeParameters();
                for (int i = 0; i < fromTypeVariables.length; i++) {
                    TypeVariable<?> fromTypeVariable = fromTypeVariables[i];
                    if (fromTypeVariable.equals(typeVariable)) {
                        return (Class<?>) fromParam.getActualTypeArguments()[i];
                    }
                }
            }
        } else {
            fromClass = (Class<?>) from;
        }
        Type superType = fromClass.getGenericSuperclass();
        if (superType != null) {
            return getTypeVariable(superType, typeVariable);
        }
        return null;
    }

    /**
     * Try getting an annotation from a property, fall back to field with the same name if not found
     *
     * @param clazz the parent class
     * @param annotationClass the annotation type to search
     * @param property the property to search for annotation
     * @param <T> generic annotation type
     * @return annotation
     */
    public static <T extends Annotation> T getAnnotation(Class<?> clazz, final Class<T> annotationClass, final PropertyDescriptor property) {
        T annotationType = property.getReadMethod().getAnnotation(annotationClass);
        if (annotationType == null) {
            // Search on the setter
            annotationType = property.getWriteMethod().getAnnotation(annotationClass);
        }
        try {
            final Map<Class<?>, T> foundFields = Maps.newHashMap();
            if (annotationType == null) {
                // Search in the field declaration
                ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
                    @Override
                    public void doWith(final Field field) throws IllegalAccessException {
                        foundFields.put(field.getDeclaringClass(), field.getAnnotation(annotationClass));

                    }
                }, new ReflectionUtils.FieldFilter() {
                    @Override
                    public boolean matches(final Field field) {
                        // no static fields please
                        return !Modifier.isStatic(field.getModifiers())
                                && (field.getName().equals(property.getName()) || field.getName().equals(property.getReadMethod().getName()));
                    }
                });
                if (!foundFields.isEmpty()) {
                    annotationType = foundFields.values().iterator().next();
                }
            }
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("Reflection error", e);
            }
        }
        return annotationType;
    }

    /**
     * Get properties descriptors
     *
     * @param clazz the class to get descriptors from
     * @return properties descriptors
     */
    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            return beanInfo.getPropertyDescriptors();
        } catch (IntrospectionException e) {
            throw new InvalidArgumentException("Cannot get descriptors of class [" + clazz.getName() + "]", e);
        }
    }

    /**
     * Merge object from an object to another
     * 
     * @param from source of the update
     * @param to target of the update
     */
    public static void mergeObject(Object from, Object to) {
        try {
            Map<String, Object> settablePropertiesMap = Maps.newHashMap();
            PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(from.getClass());
            for (PropertyDescriptor property : propertyDescriptors) {
                if (property.getReadMethod() == null || property.getWriteMethod() == null) {
                    continue;
                }
                Object value = property.getReadMethod().invoke(from);
                if (value != null) {
                    settablePropertiesMap.put(property.getName(), value);
                }
            }
            for (Map.Entry<String, Object> settableProperty : settablePropertiesMap.entrySet()) {
                // Set new values
                String propertyName = settableProperty.getKey();
                Object propertyValue = settableProperty.getValue();
                setPropertyValue(to, propertyName, propertyValue);
            }
        } catch (IllegalAccessException | InvocationTargetException | BeansException e) {
            throw new InvalidArgumentException("Cannot merge object", e);
        }
    }

    /**
     * Get property's value of an object
     * 
     * @param object the object to get property from
     * @param property the name of the property
     * @return the value of the property
     */
    public static Object getPropertyValue(Object object, String property) {
        BeanWrapper wrapper = new BeanWrapperImpl(object);
        return wrapper.getPropertyValue(property);
    }

    /**
     * Set property's value of an object
     *
     * @param object the object to set property
     * @param property the name of the property
     * @param value new value to set
     */
    public static void setPropertyValue(Object object, String property, Object value) {
        BeanWrapper wrapper = new BeanWrapperImpl(object);
        wrapper.setPropertyValue(property, value);
    }

    /**
     * Recursive getDeclaredField that allows looking for parent types fields.
     * 
     * @param fieldName The name of the field to get.
     * @param clazz The class in which to search.
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            return field;
        } catch (NoSuchFieldException e) {
            if (Object.class.equals(clazz.getSuperclass())) {
                throw e;
            }
            return getDeclaredField(clazz.getSuperclass(), fieldName);
        }
    }

    /**
     * Return true if the given class is a primitive type, a primitive wrapper or a String.
     * 
     * @param clazz The class to check.
     * @return True if the class is String, a primitive type or a primitive Wrapper type.
     */
    public static boolean isPrimitiveOrWrapperOrString(Class<?> clazz) {
        return clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || clazz.equals(Integer.class) || clazz.equals(Long.class)
                || clazz.equals(Float.class) || clazz.equals(Double.class) || clazz.equals(Character.class) || clazz.equals(Byte.class)
                || clazz.equals(Short.class);
    }
}
