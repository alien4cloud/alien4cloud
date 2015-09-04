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
public class UpdateCsarGitWithUrlRequest extends UpdateCsarGitRequest {
    @NotBlank
    @ApiModelProperty(value = "Url of the git repository before the update. This url will be used as an id to retrieve the csar git repository to update.", required = true)
    private String previousRepositoryUrl;
}
