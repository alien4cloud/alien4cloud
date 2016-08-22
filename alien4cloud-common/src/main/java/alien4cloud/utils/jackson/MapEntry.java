package alien4cloud.utils.jackson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A map/set entry as a key/value object to be serialized as is.
 * 
 * @author luc boutier
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class MapEntry<T, V> {
    private T key;
    private V value;
}