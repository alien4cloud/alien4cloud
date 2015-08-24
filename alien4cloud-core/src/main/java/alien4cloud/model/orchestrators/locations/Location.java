package alien4cloud.model.orchestrators.locations;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordnik.swagger.annotations.ApiModel;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@ESObject
@ApiModel(value = "Location", description = "A location represents a cloud, a region of a cloud, a set of machines and resources."
        + "basically any location on which alien will be allowed to perform deployment. Locations are managed by orchestrators.")
public class Location {
    @Id
    private String id;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String orchestratorId;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String infrastructureType;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String environmentType;
}