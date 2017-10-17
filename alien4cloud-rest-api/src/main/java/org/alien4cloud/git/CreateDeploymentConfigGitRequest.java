package org.alien4cloud.git;

import org.hibernate.validator.constraints.NotBlank;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@ApiModel("Request for creation of a new git repository for storing deployment config.")
public class CreateDeploymentConfigGitRequest {
    @NotBlank
    @ApiModelProperty(value = "Environment id of the related deployment config.", required = true)
    private String environmentId;

    @NotBlank
    @ApiModelProperty(value = "Url of the git repository.", required = true)
    private String url;
    @ApiModelProperty(value = "Username to access the git repository.", required = false)
    private String username;
    @ApiModelProperty(value = "Password to access the git repository.", required = false)
    private String password;
    @ApiModelProperty(value = "Path relative to the git repository where the file should be stored", required = false)
    private String path;
    @ApiModelProperty(value = "Branch to use", required = false)
    private String branch;
}