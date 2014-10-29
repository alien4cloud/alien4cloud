package alien4cloud.component.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedArtifactType extends IndexedInheritableToscaElement {
    private String mimeType;
    private List<String> fileExt;
}