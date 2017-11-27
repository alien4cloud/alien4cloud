package org.alien4cloud.tosca.utils;

import org.alien4cloud.tosca.model.types.RelationshipType;

@FunctionalInterface
public interface IRelationshipTypeFinder {
    RelationshipType findElement(String id);
}
