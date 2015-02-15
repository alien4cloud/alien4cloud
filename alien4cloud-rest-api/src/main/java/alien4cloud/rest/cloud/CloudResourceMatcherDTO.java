package alien4cloud.rest.cloud;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.cloud.CloudResourceMatcherConfig;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CloudResourceMatcherDTO {

    private CloudResourceMatcherConfig matcherConfig;

    private String[] paaSImageIds;

    private String[] paaSFlavorIds;

    private String[] paaSNetworkTemplateIds;
}
