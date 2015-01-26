package alien4cloud.paas.exception;

public class PluginNotConfiguredException extends PaaSTechnicalException {

    public PluginNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginNotConfiguredException(String message) {
        super(message);
    }
}
