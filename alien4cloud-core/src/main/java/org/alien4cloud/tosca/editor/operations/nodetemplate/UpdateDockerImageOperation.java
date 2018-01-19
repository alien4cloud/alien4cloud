package org.alien4cloud.tosca.editor.operations.nodetemplate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDockerImageOperation extends AbstractNodeOperation {
    private String dockerImage;

    @Override
    public String commitMessage() {
        return "Update docker image";
    }
}
