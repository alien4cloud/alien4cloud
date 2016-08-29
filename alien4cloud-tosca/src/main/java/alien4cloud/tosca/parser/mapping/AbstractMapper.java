package alien4cloud.tosca.parser.mapping;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.PropertyNamingStrategyBase;

import alien4cloud.tosca.parser.INodeParser;
import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.impl.base.ScalarParser;
import alien4cloud.tosca.parser.impl.base.TypeNodeParser;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMapper<T> {
    @Resource
    private ScalarParser scalarParser;

    protected final TypeNodeParser<T> instance;

    public AbstractMapper(TypeNodeParser<T> instance) {
        this.instance = instance;
    }

    /**
     * Get the instance of the mapper.
     * 
     * @return The mapper instance.
     */
    public TypeNodeParser<T> getParser() {
        return instance;
    }

    /**
     * Initialize the TypeNodeParser mapping.
     */
    @PostConstruct
    public abstract void initMapping();

    /**
     * Return the injected scalar parser.
     * 
     * @return The scalar parser.
     */
    public ScalarParser getScalarParser() {
        return scalarParser;
    }

    /**
     * Map a field to be parsed as a scalar and mapped to a tosca field that use the same name based on LOWER_CASE_WITH_UNDERSCORES rather than CAMEL_CASE.
     *
     * @param fieldName The name of the field.
     */
    public void quickMap(String fieldName) {
        int pathSeparatorLastIndex = fieldName.lastIndexOf(".");
        pathSeparatorLastIndex++;
        pathSeparatorLastIndex = pathSeparatorLastIndex > 0 ? pathSeparatorLastIndex : 0;
        String yamlFieldCamelCase = fieldName.substring(pathSeparatorLastIndex);
        String yamlField = ((PropertyNamingStrategyBase) PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES).translate(yamlFieldCamelCase);
        log.trace("Mapping yaml field {} to {} using basic ScalarParser", yamlField, fieldName);
        instance.getYamlToObjectMapping().put(yamlField, new MappingTarget(fieldName, scalarParser));
    }

    /**
     * Map a field to be parsed with the given parser and mapped to a tosca field that use the same name based on LOWER_CASE_WITH_UNDERSCORES rather than
     * CAMEL_CASE.
     *
     * @param parser The parser to use to parse the field.
     * @param fieldName The name of the field to parse.
     */
    public void quickMap(INodeParser<?> parser, String fieldName) {
        int pathSeparatorLastIndex = fieldName.lastIndexOf(".");
        pathSeparatorLastIndex++;
        pathSeparatorLastIndex = pathSeparatorLastIndex > 0 ? pathSeparatorLastIndex : 0;
        String yamlFieldCamelCase = fieldName.substring(pathSeparatorLastIndex);
        String yamlField = ((PropertyNamingStrategyBase) PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES).translate(yamlFieldCamelCase);
        log.info("Mapping yaml field {} to {} using basic ScalarParser", yamlField, fieldName);
        instance.getYamlToObjectMapping().put(yamlField, new MappingTarget(fieldName, parser));
    }
}
