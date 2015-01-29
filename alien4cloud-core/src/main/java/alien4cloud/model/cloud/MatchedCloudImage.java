package alien4cloud.model.cloud;

public class MatchedCloudImage extends AbstractMatchedResource<String> {

    public MatchedCloudImage(String imageId, String paaSResourceId) {
        super(imageId, paaSResourceId);
    }

    public MatchedCloudImage() {
    }
}
