package org.alien4cloud.tosca.editor.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

/**
 * Git repository associated with a topology.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditorGitRepository {
    /** Id of the topology. */
    @Id
    private String id;

    /** Url of the repository */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private String repositoryUrl;

    /** Username to access the repository. */
    @StringField(includeInAll = false, indexType = IndexType.no)
    private String username;

    /** Password to access the repository. */
    @StringField(includeInAll = false, indexType = IndexType.no)
    private String password;
}
