package org.alien4cloud.tosca.model.instances;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.ObjectField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Map;

import static alien4cloud.dao.model.FetchContext.SUMMARY;

/**
 * An instance of a node.
 */
@Getter
@Setter
public class NodeInstance {
    // The node template actually does not include the type version (maybe we should add that to the node template ?).
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed, includeInAll = false)
    private String typeVersion;

    @ObjectField
    private NodeTemplate nodeTemplate;

    @ObjectField(enabled = false)
    private Map<String, String> attributeValues = Maps.newHashMap();

    public void setAttribute(String key, String value) {
        attributeValues.put(key, value);
    }
}
