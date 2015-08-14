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
@ApiModel(value = "LocationResource", description = "A LocationResource ")
public class LocationResource {
    private String id;
    private String locationId;
    private LocationResourceType locationResourceType;
}
