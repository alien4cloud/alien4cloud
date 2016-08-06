package org.alien4cloud.tosca.editor.operations.substitution;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;
import org.hibernate.validator.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * Operation on a topology template.
 */
@Getter
@Setter
public abstract class AbstractTopologyTemplateOperation extends AbstractEditorOperation {

    @NotBlank
    private String topologyId;
}