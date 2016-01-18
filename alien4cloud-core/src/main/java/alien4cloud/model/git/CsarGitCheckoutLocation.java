package alien4cloud.model.git;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@ApiModel("Information of the branch and eventually folder on the branch to import as an alien csar.")
public class CsarGitCheckoutLocation {
    @NotBlank
    @ApiModelProperty(value = "Id of the git branch to import.", required = true)
    private String branchId;
    @ApiModelProperty(value = "Optional path of the location in which lies the csar to be imported.", required = false)
    private String subPath;
    @ApiModelProperty(value = "unused field.", hidden = true)
    private String lastImportedHash;
}