package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;

import java.util.List;

/**
 * Utilities to process tosca types.
 */
public class ToscaTypeUtils {

    /**
     * Check whether the node type is equals or derived from the given type name
     *
     * @param inheritableToscaType the node type
     * @param type the type name
     * @return true if the node type is equals or derived from the given type name
     */
    public static boolean isOfType(AbstractInheritableToscaType inheritableToscaType, String type) {
        return inheritableToscaType != null && (inheritableToscaType.getElementId().equals(type)
                || inheritableToscaType.getDerivedFrom() != null && inheritableToscaType.getDerivedFrom().contains(type));
    }

    /**
     * Verify that the given <code>type</code> is or inherits the given <code>expectedType</code>.
     */
    public static boolean isOfType(String type, List<String> typeHierarchy, String expectedType) {
        return expectedType.equals(type) || (typeHierarchy != null && typeHierarchy.contains(expectedType));
    }
}