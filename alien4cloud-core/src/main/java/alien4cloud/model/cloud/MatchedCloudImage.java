package alien4cloud.model.cloud;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class MatchedCloudImage extends AbstractMatchedResource<MatchedCloudImage.CloudImageId> {

    public MatchedCloudImage(CloudImageId imageId, String paaSResourceId) {
        super(imageId, paaSResourceId);
    }

    public MatchedCloudImage() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @SuppressWarnings("PMD.UnusedPrivateField")
    public static class CloudImageId implements ICloudResourceTemplate {

        private String id;
    }
}
