package org.alien4cloud.tosca.model;

import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import lombok.*;

/**
 * Defines a dependency on a CloudServiceArchive.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@RequiredArgsConstructor(suppressConstructorProperties = true)
@EqualsAndHashCode(of = { "name", "version" })
@ToString(exclude = "hash")
public class CSARDependency {
    @NonNull
    @StringField(indexType = IndexType.not_analyzed)
    private String name;

    @NonNull
    @StringField(indexType = IndexType.not_analyzed)
    private String version;

    /**
     * Hash of the main yaml file included in the csar.
     * This is used here to spot when a dependency has changed to provide update of templates as types may have changed.
     */
    private String hash;
}
