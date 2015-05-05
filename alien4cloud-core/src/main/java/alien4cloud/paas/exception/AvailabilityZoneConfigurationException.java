package alien4cloud.paas.exception;

import lombok.Getter;

public class AvailabilityZoneConfigurationException extends ResourceMatchingFailedException {

    @Getter
    private String groupId;

    public AvailabilityZoneConfigurationException(String groupId, String message) {
        super(message);
        this.groupId = groupId;
    }

}
