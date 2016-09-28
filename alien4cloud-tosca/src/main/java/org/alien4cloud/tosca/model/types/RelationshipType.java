package org.alien4cloud.tosca.model.types;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.TermsFacet;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ESObject
public class RelationshipType extends AbstractInstantiableToscaType {
    @TermsFacet
    private String[] validSources;

    @TermsFacet
    private String[] validTargets;
}