package alien4cloud.paas.model;

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
@SuppressWarnings("PMD.UnusedPrivateField")
public abstract class AbstractMonitorEvent {
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String deploymentId;
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String cloudId;
    @TermFilter
    @DateField
    @TimeStamp(format = "", index = IndexType.not_analyzed)
    private long date;
}