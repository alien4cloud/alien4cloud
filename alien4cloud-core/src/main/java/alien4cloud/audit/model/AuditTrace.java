package alien4cloud.audit.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

@Getter
@Setter
@ToString
@ESObject
public class AuditTrace {

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String category;

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
    private String method;

    @TermFilter
    @StringField(indexType = IndexType.analyzed)
    private Map<String, String[]> requestParameters;

    private String requestBody;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private int responseStatus;

    private String responseBody;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String sourceIp;
}
