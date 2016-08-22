package alien4cloud.it;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class Entry {
    private String name;
    private String value;

    @Override
    public String toString() {
        return "name: [" + name + "], value: [" + value + "]";
    }
}