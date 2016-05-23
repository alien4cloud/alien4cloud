package alien4cloud.tosca.parser.mapping.generator;

import org.yaml.snakeyaml.nodes.MappingNode;

import alien4cloud.tosca.parser.MappingTarget;
import alien4cloud.tosca.parser.ParsingContextExecution;

/**
 * Build a mapping for a given key.
 */
public interface IMappingBuilder {
    /**
     *
     * @return
     */
    String getKey();

    MappingTarget buildMapping(MappingNode mappingNode, ParsingContextExecution context);
}