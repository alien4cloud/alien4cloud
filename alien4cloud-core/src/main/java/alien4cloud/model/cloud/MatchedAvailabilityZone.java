package alien4cloud.model.cloud;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class MatchedAvailabilityZone extends AbstractMatchedResource<AvailabilityZone> {

    public MatchedAvailabilityZone(AvailabilityZone resource, String paaSResourceId) {
        super(resource, paaSResourceId);
    }

    public MatchedAvailabilityZone() {
    }
}
