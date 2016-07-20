package org.alien4cloud.tosca.editor.operations.relationshiptemplate;

import org.alien4cloud.tosca.editor.operations.nodetemplate.AbstractNodeOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Define an operation on a relationship
 */
@Getter
@Setter
public abstract class AbstractRelationshipOperation extends AbstractNodeOperation {
    @NotBlank
    private String relationshipName;
}