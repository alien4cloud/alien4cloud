package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.types.AbstractToscaType;

@FunctionalInterface
public interface IToscaTypeFinder {
    <T extends AbstractToscaType> T findElement(Class<T> clazz, String id);
}
