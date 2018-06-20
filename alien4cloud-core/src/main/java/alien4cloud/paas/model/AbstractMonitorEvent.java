package alien4cloud.paas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.TimeStamp;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ESObject
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
public abstract class AbstractMonitorEvent {
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String deploymentId;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String orchestratorId;
    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private long date;
}