package org.alien4cloud.tosca.model.types;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ESObject
public class ArtifactType extends AbstractInheritableToscaType {
    private String mimeType;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private List<String> fileExt;
}