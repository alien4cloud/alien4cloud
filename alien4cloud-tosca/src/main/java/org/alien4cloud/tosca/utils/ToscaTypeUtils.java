package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.types.AbstractInheritableToscaType;

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


}