package alien4cloud.model.components;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@SuppressWarnings("PMD.UnusedPrivateField")
public class IndexedCapabilityType extends IndexedInheritableToscaElement {
    // Index only the id of a capability type
}