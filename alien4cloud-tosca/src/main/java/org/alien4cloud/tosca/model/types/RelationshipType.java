package org.alien4cloud.tosca.model.types;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermsFacet;

import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ESObject
public class RelationshipType extends AbstractInstantiableToscaType {
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    @TermsFacet
    private String[] validSources;

    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    @TermsFacet
    private String[] validTargets;
}