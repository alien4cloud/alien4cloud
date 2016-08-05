package alien4cloud.utils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.util.ReflectionUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.exception.InvalidArgumentException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to help working with java reflection.
 */
@Slf4j
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    /**
     * Get the Class of the generic argument of a generic class based on it's implementation class.
     *
     * @param implementationClass The implementation class that actually is a sub-class of generic class.
     * @param genericClass The generic class for which to get the argument type.
     * @param index The index of the argument.
     * @return null if the type cannot be found, the generic type when found.
     */
    public static <T> Class<?> getGenericArgumentType(Class<? extends T> implementationClass, Class<T> genericClass, int index) {
        if (implementationClass.isAssignableFrom(genericClass)) {
            return null;
        }

        Type[] types = getGenericArgumentTypes(implementationClass, genericClass, implementationClass.getTypeParameters());
        if (types != null) {
            return (Class<?>) types[index];
        }

        return null;
    }

    private static <T> Type[] getGenericArgumentTypes(Class<? extends T> currentClass, Class<T> genericClass, Type... resolvedTypes) {
        Map<String, Type> resolvedTypesByName = new HashMap<String, Type>();
        for (int i = 0; i < resolvedTypes.length; i++) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) currentClass.getTypeParameters()[i];
            resolvedTypesByName.put(typeVariable.getName(), resolvedTypes[i]);
        }

        Set<Type> directGenericInterfaces = getDirectGenericInterfaces(currentClass, genericClass);
        Type[] types = null;
        for (Type genericInterface : directGenericInterfaces) {
            if (genericInterface instanceof Class) {
                types = getGenericArgumentTypes((Class<? extends T>) genericInterface, genericClass);
            }
            if (types == null && genericInterface instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                List<Type> nextResolvedTypes = new LinkedList<Type>();
                for (Type t : parameterizedType.getActualTypeArguments()) {
                    if (t instanceof TypeVariable<?>) {
                        Type resolvedType = resolvedTypesByName.get(((TypeVariable<?>) t).getName());
                        nextResolvedTypes.add(resolvedType != null ? resolvedType : t);
                    } else {
                        nextResolvedTypes.add(t);
                    }
                }

                types = getGenericArgumentTypes((Class<? extends T>) parameterizedType.getRawType(), genericClass,
                        nextResolvedTypes.toArray(new Type[nextResolvedTypes.size()]));
            }
            if (types != null) {
                return types;
            }
        }

        return resolvedTypes;
    }

    /**
     * Get all direct generic interfaces that can be assigned from the expected class.
     * 
     * @param clazz The class from which to get generic.
     * @param expectedClazz The class from which .
     * @return a Set that contains all direct generic interfaces matching the expectedClazz from clazz.
     */
    private static Set<Type> getDirectGenericInterfaces(Class<?> clazz, Class<?> expectedClazz) {
        Set<Type> superTypes = Sets.newHashSet();
        if (clazz.getGenericSuperclass() != null && isAssignableFrom(clazz.getGenericSuperclass(), expectedClazz)) {
            superTypes.add(clazz.getGenericSuperclass());
        }
        for (Type genericInterface : clazz.getGenericInterfaces()) {
            if (isAssignableFrom(genericInterface, expectedClazz)) {
                superTypes.add(genericInterface);
            }
        }
        return superTypes;
    }

    private static boolean isAssignableFrom(Type type, Class<?> clazz) {
        if (type instanceof Class) {
            return clazz.isAssignableFrom((Class<?>) type);
        }
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return rawType instanceof Class && clazz.isAssignableFrom((Class<?>) rawType);
        }
        return false;
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
     * Merge object from an object to another. Failsafe : resist to invalid property.
     * 
     * @param from source of the update
     * @param to target of the update
     * @param ignores properties names that should be ignored
     */
    public static void mergeObject(Object from, Object to, String... ignores) {
        Set<String> ignoredProps = Sets.newHashSet(ignores);
        try {
            Map<String, Object> settablePropertiesMap = Maps.newHashMap();
            PropertyDescriptor[] propertyDescriptors = getPropertyDescriptors(from.getClass());
            for (PropertyDescriptor property : propertyDescriptors) {
                if (property.getReadMethod() == null || property.getWriteMethod() == null) {
                    continue;
                }
                Object value = property.getReadMethod().invoke(from);
                if (value != null && !ignoredProps.contains(property.getName())) {
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
        if (wrapper.isWritableProperty(property)) {
            wrapper.setPropertyValue(property, value);
        }
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
