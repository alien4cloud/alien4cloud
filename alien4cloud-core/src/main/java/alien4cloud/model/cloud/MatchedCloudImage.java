package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class MatchedCloudImage extends AbstractMatchedResource<CloudImage> {

    public MatchedCloudImage(CloudImage resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedCloudImage() {
    }
}
