package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class MatchedNetworkTemplate extends AbstractMatchedResource<NetworkTemplate> {

    public MatchedNetworkTemplate(NetworkTemplate resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedNetworkTemplate() {
    }
}
