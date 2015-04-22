package alien4cloud.model.topology;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@ESObject
@NoArgsConstructor
public class Policy {

    private String name;

    private String type;

}
