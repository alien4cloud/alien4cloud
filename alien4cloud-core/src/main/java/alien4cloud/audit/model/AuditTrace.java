package alien4cloud.audit.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.DateField;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ToString
@ESObject
public class AuditTrace {

    @TermFilter
    @DateField(includeInAll = false, index = IndexType.no)
    private long timestamp;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @TermsFacet
    private String category;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @TermsFacet
    private String action;

    @StringField(indexType = IndexType.analyzed)
    private String actionDescription;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String userName;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String userEmail;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String userFirstName;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String userLastName;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String path;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @TermsFacet
    private String method;

    @TermFilter
    @StringField(indexType = IndexType.analyzed)
    private Map<String, String[]> requestParameters;

    @StringField(indexType = IndexType.analyzed)
    private String requestBody;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    @TermsFacet
    private int responseStatus;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String sourceIp;

}
