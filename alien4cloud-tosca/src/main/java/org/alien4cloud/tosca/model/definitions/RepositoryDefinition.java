package org.alien4cloud.tosca.model.definitions;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
@EqualsAndHashCode(of = { "id" })
public class RepositoryDefinition {
    private String id;
    /**
     * Type is not tosca, this is an Alien meta data tag to boost artifact resolution
     */
    private String type;
    private String description;
    private String url;
    private ComplexPropertyValue credential;

    public RepositoryDefinition(String url) {
        this.url = url;
    }
}
