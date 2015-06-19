package alien4cloud.security.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.elasticsearch.annotation.ESObject;
import org.hibernate.validator.constraints.NotBlank;

@ESObject
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CsarGitCheckoutLocation {
    @NotBlank
    private String branchId;

    private String subPath;
}
