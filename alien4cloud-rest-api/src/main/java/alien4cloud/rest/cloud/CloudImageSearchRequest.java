package alien4cloud.rest.cloud;

import java.util.Set;

import alien4cloud.rest.model.BasicSearchRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudImageSearchRequest extends BasicSearchRequest {
    private Set<String> exclude;
}
