package org.alien4cloud.tosca.model.templates;

import org.elasticsearch.annotation.BooleanField;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.security.IManagedSecuredResource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTopologyVersion implements IManagedSecuredResource {
    @Id
    private String id; // Id is both the id of the version and of the archive that back this version.
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String version;
    private String description;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean released;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean latest;
    @BooleanField(index = IndexType.not_analyzed)
    private boolean isSnapshot;

    public abstract void setDelegateId(String id);

}