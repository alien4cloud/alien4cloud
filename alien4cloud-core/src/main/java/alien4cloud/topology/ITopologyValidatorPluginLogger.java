package alien4cloud.topology;

public interface ITopologyValidatorPluginLogger {

    void error(String message);
    void warn(String message);
    void info(String message);
}
