package alien4cloud.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by xdegenne on 04/11/2016.
 */
@AllArgsConstructor
@Getter
@Setter
public class AvailableTopologyRepository {

    private String id;
    private String type;
    private String url;

}
