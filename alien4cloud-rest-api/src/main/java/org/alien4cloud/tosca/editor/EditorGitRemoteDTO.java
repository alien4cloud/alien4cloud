package org.alien4cloud.tosca.editor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class EditorGitRemoteDTO {

    private String remoteName;
    private String remoteUrl;
}
