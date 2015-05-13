package alien4cloud.security.model;

import java.util.Set;

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

@ESObject
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class Group {
    @Id
    private String id;
    @TermFilter
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    private String name;
    @StringField(includeInAll = false, indexType = IndexType.no)
    private String email;
    private String description;
    private Set<String> users;
    private Set<String> roles;

    public Group(String name) {
        this.name = name;
    }

}
