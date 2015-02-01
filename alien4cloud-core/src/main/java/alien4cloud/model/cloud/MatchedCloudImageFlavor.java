package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class MatchedCloudImageFlavor extends AbstractMatchedResource<CloudImageFlavor> {

    public MatchedCloudImageFlavor(CloudImageFlavor resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedCloudImageFlavor() {
    }
}
