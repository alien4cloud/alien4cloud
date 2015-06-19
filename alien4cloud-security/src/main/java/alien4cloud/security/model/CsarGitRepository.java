package alien4cloud.security.model;

import java.util.List;
import java.util.UUID;

import lombok.Data;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@ESObject
@Data
@JsonInclude(Include.NON_NULL)
public class CsarGitRepository {

    /**
     * Technical internal id
     */
    @Id
    private String id;

    @StringField(includeInAll = false, indexType = IndexType.no)
    @TermFilter
    private String repositoryUrl;

    @StringField(includeInAll = false, indexType = IndexType.no)
    private String username;

    @StringField(includeInAll = false, indexType = IndexType.no)
    private String password;

    private List<CsarGitCheckoutLocation> importLocations;

    public CsarGitRepository() {
        this.id = UUID.randomUUID().toString();
    }

    public void setId() {
        // Do nothing, technical id was set at creation
    }
}
