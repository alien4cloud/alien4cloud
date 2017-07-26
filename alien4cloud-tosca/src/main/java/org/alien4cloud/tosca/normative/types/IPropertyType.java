package org.alien4cloud.tosca.normative.types;

import org.alien4cloud.tosca.exceptions.InvalidPropertyValueException;

/**
 * This class represents all normative property type as string, integer, scalar-unit.size ...
 */
public interface IPropertyType<T> {

    T parse(String text) throws InvalidPropertyValueException;

    String print(T value);

    String getTypeName();
}