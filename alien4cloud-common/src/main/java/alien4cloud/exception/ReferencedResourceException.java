package alien4cloud.exception;

import alien4cloud.model.common.Usage;
import lombok.Getter;

/**
 * Exception to be thrown when an operation cannot be done because a resource is used (referenced).
 */
@Getter
public class ReferencedResourceException extends TechnicalException {
    private final Usage[] usages;

    public ReferencedResourceException(String message, Usage[] usages) {
        super(message);
        this.usages = usages;
    }
}
