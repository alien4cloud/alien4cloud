package alien4cloud.model.topology;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractPolicy {

    private String name;

    public abstract String getType();

    public void setType(String type) {
        // here only for JSON deserialization
    }

}
