package org.alien4cloud.tosca.editor.operations;

import alien4cloud.utils.AlienUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.alien4cloud.tosca.model.CSARDependency;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * Change a dependency version.
 */
@Getter
@Setter
@NoArgsConstructor
public class ChangeDependencyVersionOperation extends AbstractEditorOperation {

    /**
     * The dependency name.
     */
    private String dependencyName;

    /**
     * The new dependency version.
     */
    private String dependencyVersion;

    @Override
    public String commitMessage() {
        return String.format("Change depenpency <%s> version to <%s>", dependencyName, dependencyVersion);
    }
}