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
@SuppressWarnings("PMD.UnusedPrivateField")
public class ComputeTemplate {

    private String cloudImageId;

    private String cloudImageFlavorId;

    private boolean enabled = true;

    public ComputeTemplate(String cloudImageId, String cloudImageFlavorId) {
        this.cloudImageId = cloudImageId;
        this.cloudImageFlavorId = cloudImageFlavorId;
    }
}
