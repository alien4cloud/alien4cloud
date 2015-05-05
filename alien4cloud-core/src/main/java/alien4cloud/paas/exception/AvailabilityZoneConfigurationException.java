package alien4cloud.paas.exception;

public class AvailabilityZoneConfigurationException extends ResourceMatchingFailedException {

    public AvailabilityZoneConfigurationException(String message) {
        super(message);
    }

    public AvailabilityZoneConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
