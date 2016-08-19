package alien4cloud.rest.suggestion;

import java.util.Set;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@AllArgsConstructor(suppressConstructorProperties = true)
@NoArgsConstructor
@ApiModel("Creation request for a suggestion.")
public class CreateSuggestionEntryRequest {

    @ApiModelProperty(value = "Id of elasticsearch index where, the property to be suggested, is located .")
    @NotNull
    private String esIndex;

    @ApiModelProperty(value = "Id of elasticsearch type where, the property to be suggested, is located.")
    @NotNull
    private String esType;

    @ApiModelProperty(value = "Id of the element where, the property to be suggested, is located.")
    @NotNull
    private String targetElementId;

    @ApiModelProperty(value = "Id of the property to be suggested.")
    @NotNull
    private String targetProperty;

    @ApiModelProperty(value = "List of initial values for suggestions.")
    private Set<String> suggestions;
}
