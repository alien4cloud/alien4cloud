package org.alien4cloud.tosca.editor.commands;

import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Define an operation on a relationship
 */
@Getter
@Setter
public class AbstractRelationshipOperation extends AbstractNodeOperation {
    @NotBlank
    private String relationshipName;
}