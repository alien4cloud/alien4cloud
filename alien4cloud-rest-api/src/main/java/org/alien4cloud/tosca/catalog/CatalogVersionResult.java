package org.alien4cloud.tosca.catalog;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Element that associate a version and id of the version for an element in the catalog (tosca type, topology or csar).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogVersionResult {
    /** The id of the element. */
    private String id;
    /** The version of the element. */
    private String version;
}