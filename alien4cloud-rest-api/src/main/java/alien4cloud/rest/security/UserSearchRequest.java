package alien4cloud.rest.security;

import alien4cloud.rest.model.BasicSearchRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSearchRequest extends BasicSearchRequest {
    private String group;

    /** No arg constructor. */
    public UserSearchRequest() {
    }

    public UserSearchRequest(String query, String group, int from, int size) {
        super(query, from, size);
        this.group = group;
    }
}