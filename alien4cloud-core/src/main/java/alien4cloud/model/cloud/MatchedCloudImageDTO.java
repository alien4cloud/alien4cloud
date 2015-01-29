package alien4cloud.model.cloud;

public class MatchedCloudImageDTO extends AbstractMatchedResource<CloudImage> {

    public MatchedCloudImageDTO(CloudImage resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedCloudImageDTO() {
    }
}
