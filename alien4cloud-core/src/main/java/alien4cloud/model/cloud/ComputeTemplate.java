package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
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
