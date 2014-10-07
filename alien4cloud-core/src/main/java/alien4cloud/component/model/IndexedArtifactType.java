package alien4cloud.component.model;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedArtifactType extends IndexedInheritableToscaElement {
    // Index only the id of a artifact type
}
