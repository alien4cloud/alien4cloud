package org.alien4cloud.alm.deployment.configuration.model;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.IDatableResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import java.util.Date;

/**
 * Deployment configurations objects (inputs, matching data) should inherit from this class.
 */
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractDeploymentConfig implements IDatableResource {
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String versionId;
    @TermFilter
    @StringField(includeInAll = false, indexType = IndexType.not_analyzed)
    private String environmentId;

    @Id
    public String getId() {
        return generateId(versionId, environmentId);
    }

    private Date creationDate;

    private Date lastUpdateDate;

    public AbstractDeploymentConfig(String versionId, String environmentId) {
        this.versionId = versionId;
        this.environmentId = environmentId;
    }

    public void setId(String id) {
        // Do nothing as id is generated.
    }

    /**
     * Utility method to generate an id for a deployment topology by concatenating version id and environment id
     *
     * @param versionId     id of the version
     * @param environmentId id of the environment
     * @return concatenated id
     */
    public static String generateId(String versionId, String environmentId) {
        if (versionId == null) {
            throw new IndexingServiceException("version id is mandatory");
        }
        if (environmentId == null) {
            throw new IndexingServiceException("environment id is mandatory");
        }
        return versionId + "::" + environmentId;
    }

    /**
     * Utility method to extract the topology version and environment id from a {@link AbstractDeploymentConfig} id.
     *
     * @param id id to parse
     * @return the topology id and environment id
     */
    public static VersionIdEnvId extractInfoFromId(String id) {
        String[] split = StringUtils.split(id, "::");
        if (split.length != 2) {
            throw new IllegalStateException("id <" + id + "> is not valid");
        }
        return new VersionIdEnvId(split[0], split[1]);
    }

    @Getter
    @AllArgsConstructor
    public static class VersionIdEnvId {
        private String versionId;
        private String environmentId;
    }
}
