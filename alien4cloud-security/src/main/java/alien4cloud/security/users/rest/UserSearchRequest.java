package alien4cloud.security.users.rest;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.rest.model.BasicSearchRequest;

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