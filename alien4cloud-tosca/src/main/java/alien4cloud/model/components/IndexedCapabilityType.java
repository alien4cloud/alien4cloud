package alien4cloud.model.components;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.query.TermsFacet;

@Getter
@Setter
@ESObject
public class IndexedCapabilityType extends IndexedInheritableToscaElement {
    @TermsFacet
    private String[] validSources;
}