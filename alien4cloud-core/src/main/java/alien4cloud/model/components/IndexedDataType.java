package alien4cloud.model.components;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

/**
 * Complex data type used for property definition
 */
@Getter
@Setter
@EqualsAndHashCode(of = {}, callSuper = true)
@ESObject
public class IndexedDataType extends IndexedInheritableToscaElement {
}
