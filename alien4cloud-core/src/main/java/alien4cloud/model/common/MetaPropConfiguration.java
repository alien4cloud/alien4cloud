package alien4cloud.model.common;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermsFacet;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.ui.form.annotation.FormLabel;
import alien4cloud.ui.form.annotation.FormProperties;
import alien4cloud.ui.form.annotation.FormValidValues;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Predefined configuration for tag edit
 */
@ESObject
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@JsonInclude(JsonInclude.Include.NON_NULL)
@FormProperties({ "name", "description", "required", "target", "type", "password", "default", "constraints" })
public class MetaPropConfiguration extends PropertyDefinition {
    /**
     * The name of the tag
     */
    @StringField(includeInAll = true, indexType = IndexType.analyzed)
    @NotNull
    @FormLabel("TAG_CONFIG.NAME")
    private String name;

    /**
     * Target of the tag configuration (application or component)
     */
    @StringField(includeInAll = true, indexType = IndexType.not_analyzed)
    @FormValidValues({ "application", "component", "cloud" })
    @NotNull
    @TermsFacet
    @FormLabel("TAG_CONFIG.TARGET")
    private String target;

    /**
     * Auto generated id
     */
    @Id
    private String id;
}