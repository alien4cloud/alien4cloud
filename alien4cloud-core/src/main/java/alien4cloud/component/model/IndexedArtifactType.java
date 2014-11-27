package alien4cloud.component.model;

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
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedArtifactType extends IndexedInheritableToscaElement {
    private String mimeType;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private List<String> fileExt;
}