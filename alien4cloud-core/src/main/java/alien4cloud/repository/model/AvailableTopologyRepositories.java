package alien4cloud.repository.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by xdegenne on 04/11/2016.
 */
@AllArgsConstructor
@Getter
@Setter
public class AvailableTopologyRepositories {

    private List<AvailableTopologyRepository> archiveRepository;
    private List<AvailableTopologyRepository> alienRepository;
    private List<String> repositoryTypes;

}
