package alien4cloud.model.cloud;

public class MatchedCloudImage extends AbstractMatchedResource<CloudImage> {

    public MatchedCloudImage(CloudImage resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedCloudImage() {
    }
}
