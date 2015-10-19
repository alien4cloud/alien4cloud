package alien4cloud.security.model;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@ESObject
public class CsarGitRepository {
    @Id
    private String id;

    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    @TermFilter
    private String repositoryUrl;

    @StringField(includeInAll = false, indexType = IndexType.no)
    private String username;

    @StringField(includeInAll = false, indexType = IndexType.no)
    private String password;

    private List<CsarGitCheckoutLocation> importLocations;

    private boolean storedLocally;
}
