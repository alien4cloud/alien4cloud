package alien4cloud.component.repository.exception;

import alien4cloud.exception.TechnicalException;

public class InvalidResolverConfigurationException extends TechnicalException {

    public InvalidResolverConfigurationException(String message) {
        super(message);
    }
}
