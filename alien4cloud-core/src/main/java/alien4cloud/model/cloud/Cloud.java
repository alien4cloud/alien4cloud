package alien4cloud.model.cloud;

import static alien4cloud.dao.model.FetchContext.DEPLOYMENT;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.NestedObject;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.FetchContext;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;
import org.hibernate.validator.constraints.NotBlank;

import alien4cloud.model.application.EnvironmentType;
import alien4cloud.security.ISecuredResource;
import alien4cloud.utils.JSonMapEntryArrayDeSerializer;
import alien4cloud.utils.JSonMapEntryArraySerializer;
import alien4cloud.utils.NotAnalyzedTextMapEntry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Sets;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("PMD.UnusedPrivateField")
@ESObject
@ApiModel(value = "A cloud definition in ALIEN", description = "Defines a cloud in ALIEN.")
public class Cloud implements ISecuredResource {
    /**
     * Name of the cloud.
     */
    @Id
    private String id;
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String name;
    /**
     * Id of the plugin that contains the PaaSProvider that manages this cloud.
     */
    @NotBlank
    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private String paasPluginId;
    /**
     * Name of the IPaaSProvider bean that manages the cloud.
     */
    @NotBlank
    @StringField(indexType = IndexType.not_analyzed)
    private String paasPluginBean;
    @StringField(indexType = IndexType.not_analyzed)
    private String paasProviderName;
    @ApiModelProperty(hidden = true)
    private boolean isConfigurable;
    /**
     * Type of environment for the cloud.
     */
    private IaaSType iaaSType;
    /**
     * Type of environment for the cloud.
     */
    private EnvironmentType environmentType;
    /**
     * Flag to know if the cloud is currently enabled.
     */
    @TermFilter
    @ApiModelProperty(hidden = true)
    private boolean enabled;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
    private Map<String, Set<String>> userRoles;

    @TermFilter(paths = { "key", "value" })
    @NestedObject(nestedClass = NotAnalyzedTextMapEntry.class)
    @JsonDeserialize(using = JSonMapEntryArrayDeSerializer.class)
    @JsonSerialize(using = JSonMapEntryArraySerializer.class)
    @FetchContext(contexts = { DEPLOYMENT }, include = { true })
    private Map<String, Set<String>> groupRoles;

    @TermFilter
    @StringField(indexType = IndexType.not_analyzed)
    private Set<String> images = Sets.newLinkedHashSet();

    private Set<CloudImageFlavor> flavors = Sets.newLinkedHashSet();

    private Set<NetworkTemplate> networks = Sets.newLinkedHashSet();
}
