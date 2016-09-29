package alien4cloud.rest.csar;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.common.Usage;
import org.alien4cloud.tosca.model.Csar;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(suppressConstructorProperties = true)
public class CsarInfoDTO {
    private Csar csar;
    private List<Usage> relatedResources;
}
