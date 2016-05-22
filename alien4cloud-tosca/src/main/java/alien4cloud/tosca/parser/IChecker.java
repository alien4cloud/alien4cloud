package alien4cloud.tosca.parser;

import org.yaml.snakeyaml.nodes.Node;

import alien4cloud.tosca.parser.impl.base.TypeNodeParser;

/**
 * A checker is responsible of checking the validity of an object. It can also populate the object in order to enrich it.
 * Can be use with a {@link TypeNodeParser} by adding |checkerName after the java type in mapping configuration.
 */
public interface IChecker<T> {

    /** this is the name that is used in the mapping configuration. **/
    String getName();

    void before(ParsingContextExecution context, Node node);

    void check(T instance, ParsingContextExecution context, Node node);

}
