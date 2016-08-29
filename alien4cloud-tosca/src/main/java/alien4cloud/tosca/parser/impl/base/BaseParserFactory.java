package alien4cloud.tosca.parser.impl.base;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import alien4cloud.tosca.parser.INodeParser;

/**
 * Allow to retrieve instances of base parsers from the application context. Allowing to get new instances of beans (prototype scoped) with AOP support.
 */
@Component
public class BaseParserFactory {
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Get a new instance of a ReferencedParser.
     *
     * @param typeName The name of the type in the parser registry to use for the parsing.
     * @return a new instance of the ReferencedParser.
     */
    public ReferencedParser getReferencedParser(String typeName) {
        return applicationContext.getBean(ReferencedParser.class, typeName);
    }

    /**
     * Get a new instance of a TypeNodeParser.
     * 
     * @param type The type of the object to parse.
     * @param toscaType The name of the type in tosca.
     * @param <T> The parsed object type.
     * @return a new instance of the TypeNodeParser.
     */
    public <T> TypeNodeParser getTypeNodeParser(Class<T> type, String toscaType) {
        return applicationContext.getBean(TypeNodeParser.class, type, toscaType);
    }

    /**
     * Get a new instance of a KeyDiscriminatorParser with the given parameters for key discrimined parsers and fallback.
     * 
     * @param parsersByKey The parsers to associate based on key found in the parsed node.
     * @param fallbackParser The fallback parser in case no key matches.
     * @return a new instance of the KeyDiscriminatorParser
     */
    public KeyDiscriminatorParser getKeyDiscriminatorParser(Map<String, INodeParser<?>> parsersByKey, INodeParser<?> fallbackParser) {
        return applicationContext.getBean(KeyDiscriminatorParser.class, parsersByKey, fallbackParser);
    }

    /**
     * Get a new instance of a ListParser.
     *
     * @param valueParser The parser to use to parse list values.
     * @param toscaType The expected type name to generate error messages.
     * @param <T> The type of the list values.
     * @return a new instance of the ListParser
     */
    public <T> ListParser<T> getListParser(INodeParser<T> valueParser, String toscaType) {
        return applicationContext.getBean(ListParser.class, valueParser, toscaType);
    }

    /**
     * Get a new instance of a ListParser.
     *
     * @param valueParser The parser to use to parse list values.
     * @param toscaType The expected type name to generate error messages.
     * @param keyPath In case the list is created from a map, optional value to inject the key into the value object.
     * @param <T> The type of the list values.
     * @return a new instance of the ListParser
     */
    public <T> ListParser<T> getListParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        return applicationContext.getBean(ListParser.class, valueParser, toscaType, keyPath);
    }

    /**
     * Get a new instance of a SetParser.
     *
     * @param valueParser The parser to use to parse set values.
     * @param toscaType The tosca type of the set.
     * @param <T> The type of the set values.
     * @return a new instance of the SetParser
     */
    public <T> SetParser getSetParser(INodeParser<T> valueParser, String toscaType) {
        return applicationContext.getBean(SetParser.class, valueParser, toscaType);
    }

    /**
     * Get a new instance of a SetParser.
     *
     * @param valueParser The parser to use to parse set values.
     * @param toscaType The tosca type of the set.
     * @param keyPath In case the list is created from a map, optional value to inject the key into the value object.
     * @param <T> The type of the set values.
     * @return a new instance of the SetParser
     */
    public <T> SetParser getSetParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        return applicationContext.getBean(SetParser.class, valueParser, toscaType, keyPath);
    }

    /**
     * Get a new instance of a MapParser.
     *
     * @param valueParser The parser to use to parse map values.
     * @param toscaType The expected type name to generate error messages.
     * @param <T> The type of the map values.
     * @return a new instance of the MapParser
     */
    public <T> MapParser getMapParser(INodeParser<T> valueParser, String toscaType) {
        return (MapParser) applicationContext.getBean("mapParser", valueParser, toscaType);
    }

    /**
     * Get a new instance of a MapParser.
     * 
     * @param valueParser The parser to use to parse map values.
     * @param toscaType The expected type name to generate error messages.
     * @param keyPath Optional value to inject the key into the value object.
     * @param <T> The type of the map values.
     * @return a new instance of the MapParser
     */
    public <T> MapParser getMapParser(INodeParser<T> valueParser, String toscaType, String keyPath) {
        return (MapParser) applicationContext.getBean("mapParser", valueParser, toscaType, keyPath);
    }

    /**
     * Get a new instance of a SequenceToMapParser.
     *
     * @param valueParser The parser to use to parse map values.
     * @param toscaType The tosca type of the map.
     * @param nodeIsValue If the sequence element mapping node is also the node of the value (both key and value).
     * @param <T> The type of the map values.
     * @return a new instance of the SequenceToMapParser
     */
    public <T> SequenceToMapParser getSequenceToMapParser(INodeParser<T> valueParser, String toscaType, Boolean nodeIsValue) {
        return applicationContext.getBean(SequenceToMapParser.class, valueParser, toscaType, nodeIsValue);
    }
}