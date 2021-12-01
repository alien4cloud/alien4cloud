package alien4cloud.model.suggestion;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "value")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Suggestion {

    private String value;

    private String description;

}
