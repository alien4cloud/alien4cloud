package alien4cloud.rest.topology;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import io.swagger.annotations.ApiModelProperty;

/**
 * Posted object to update a relationship property
 * 
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class UpdateIndexedTypePropertyRequest {
    @ApiModelProperty(required = true)
    private String propertyName;
    @ApiModelProperty(required = true)
    private String propertyValue;
    @ApiModelProperty(required = true)
    private String type;
}
