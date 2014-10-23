package alien4cloud.cloud;

import java.util.List;
import java.util.Map;

import alien4cloud.model.cloud.ActivableComputeTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.CloudImage;
import alien4cloud.model.cloud.CloudImageFlavor;

import com.fasterxml.jackson.annotation.JsonInclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
public class CloudResourceTopologyMatchResult {

    private Map<String, CloudImage> images;

    private Map<String, CloudImageFlavor> flavors;

    private Map<String, List<ActivableComputeTemplate>> matchResult;
}
