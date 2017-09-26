package alien4cloud.plugin.exception;

import alien4cloud.exception.FunctionalException;

/**
 * Exception to be thrown in case we fail to load a plugin in Alien.
 *
 * @author luc boutier
 */
public class PluginLoadingException extends FunctionalException {
    private static final long serialVersionUID = 1L;

    public PluginLoadingException(String message) {
        super(message);
    }

    public PluginLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
