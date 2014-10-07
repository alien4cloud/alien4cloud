package alien4cloud.model.cloud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class ComputeTemplate {

    private String cloudImageId;

    private String cloudImageFlavorId;

    private boolean enabled = true;
}
