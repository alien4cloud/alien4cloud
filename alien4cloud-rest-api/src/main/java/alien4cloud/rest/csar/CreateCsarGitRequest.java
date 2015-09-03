package alien4cloud.rest.csar;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.security.model.CsarGitCheckoutLocation;
import org.hibernate.validator.constraints.NotEmpty;

@Getter
@Setter
@NoArgsConstructor
@ApiModel("Request for creation of a new csar git repository.")
public class CreateCsarGitRequest {
    @NotBlank
    @ApiModelProperty(value = "Url of the git repository.", required = true)
    private String repositoryUrl;

    @ApiModelProperty(value = "Username to access the git repository.", required = false)
    private String username;
    @ApiModelProperty(value = "Password to access the git repository.", required = false)
    private String password;
    @ApiModelProperty(value = "Flag to know if the repository should be kept on the alien4cloud server disk (so next imports will be faster).", required = false)
    private boolean storedLocally;

    @NotNull
    @NotEmpty
    @ApiModelProperty(value = "Information of branches and eventually folders to import for the given repository.", required = true)
    private List<CsarGitCheckoutLocation> importLocations;
}