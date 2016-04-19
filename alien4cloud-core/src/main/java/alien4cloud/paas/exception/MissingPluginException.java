package alien4cloud.paas.exception;

import alien4cloud.exception.TechnicalException;
import lombok.Getter;

/**
 * Exception thrown in case a plugin cannot be found.
 */
public class MissingPluginException extends TechnicalException {
    private static final long serialVersionUID = 1L;
    /** If true this means that the plugin has been found but the plugin bean hasn't been found. If false the plugin has not been found. */
    @Getter
    private boolean missingBean;

    /**
     * New instance of the exception.
     * 
     * @param message Message.
     * @param missingBean If true this means that the plugin has been found but the plugin bean hasn't been found. If false the plugin has not been found.
     */
    public MissingPluginException(String message, boolean missingBean) {
        super(message);
        this.missingBean = missingBean;
    }
}