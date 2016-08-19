package org.alien4cloud.tosca.editor.model;

import java.util.Map;

import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Information of remote git associated with a topology.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class EditorGitRepository {
    /** Id of the archive under edition. */
    @Id
    private String id;

    /** Map url, name of the remote repositories associated with the archive under edition */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @TermFilter
    private Map<String, String> namesByRemoteUrl;
}