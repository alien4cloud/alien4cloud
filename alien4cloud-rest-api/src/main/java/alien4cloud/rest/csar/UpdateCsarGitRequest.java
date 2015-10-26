package alien4cloud.rest.csar;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@ApiModel("Request for creation of a new csar git repository.")
public class UpdateCsarGitRequest {
    @ApiModelProperty(value = "Url of the git repository.", required = false)
    private String repositoryUrl;
    @ApiModelProperty(value = "Username to access the git repository.", required = false)
    private String username;
    @ApiModelProperty(value = "Password to access the git repository.", required = false)
    private String password;
}