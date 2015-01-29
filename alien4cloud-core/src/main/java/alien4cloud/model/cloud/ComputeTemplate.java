package alien4cloud.model.cloud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ComputeTemplate implements ICloudResourceTemplate {

    private String cloudImageId;

    private String cloudImageFlavorId;

    @Override
    public String getId() {
        return cloudImageId + cloudImageFlavorId;
    }
}