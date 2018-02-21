package org.alien4cloud.tosca.editor.operations.variable;

import java.util.List;

import org.alien4cloud.tosca.editor.operations.AbstractEditorOperation;

import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;

/**
 * Delete the variable from all topology related scopes (environment, environment type).
 */
@Getter
@Setter
public class DeleteTopologyVariableOperation extends AbstractEditorOperation {
    private String name;
    private List<AbstractUpdateTopologyVariableOperation> operations = Lists.newArrayList();

    @Override
    public String commitMessage() {
        return "Deleted variable <" + name + "> from all topology's scopes.";
    }
}
