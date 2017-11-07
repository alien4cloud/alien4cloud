package alien4cloud.plugin.exception;

import alien4cloud.exception.FunctionalException;

/**
 * Exception to be thrown in case we fail to do parse or load an archive provided by a plugin in Alien.
 *
 * @author igor
 */
public class PluginArchiveException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    public PluginArchiveException(String message) {
        super(message);
    }

    public PluginArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
