package org.alien4cloud.alm.deployment.configuration.model;

import java.util.Date;

import org.elasticsearch.annotation.Id;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.annotation.query.TermFilter;
import org.elasticsearch.mapping.IndexType;

import alien4cloud.exception.IndexingServiceException;
import alien4cloud.model.common.IDatableResource;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
     * @param versionId id of the version
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
}
