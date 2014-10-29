package alien4cloud.tosca.parser;

/**
 * Interface to be implemented by every parsers.
 */
public interface IParser {
    /**
     * If true the parser will be executed after all other parsers have been completed.
     * 
     * @return True if deffered, false if not.
     */
    boolean isDeffered();
}
