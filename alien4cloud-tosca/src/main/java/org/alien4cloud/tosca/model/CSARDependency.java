package org.alien4cloud.tosca.model;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Defines a dependency on a CloudServiceArchive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(of = { "name", "version" })
@ToString(exclude = "hash")
public class CSARDependency {
    @NonNull
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    @NonNull
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String version;

    /**
     * Hash of the main yaml file included in the csar.
     * This is used here to spot when a dependency has changed to provide update of templates as types may have changed.
     */
    @StringField(indexType = IndexType.no)
    private String hash;
}