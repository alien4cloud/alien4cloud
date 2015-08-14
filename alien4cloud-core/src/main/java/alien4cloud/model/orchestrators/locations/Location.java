package alien4cloud.model.orchestrators.locations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.annotation.ESObject;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
@ApiModel(value = "Location", description = "A location represents a cloud, a region of a cloud, a set of machines and resources."
        + "basically any location on which alien will be allowed to perform deployment. Locations are managed by orchestrators.")
public class Location {
    private String id;
    private String orchestratorId;
}