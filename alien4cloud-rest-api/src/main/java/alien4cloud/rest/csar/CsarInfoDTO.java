package alien4cloud.rest.csar;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.Csar;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CsarInfoDTO {
    private Csar csar;
    private List<Object> relatedResources;
}
