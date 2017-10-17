package org.alien4cloud.git.model;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import alien4cloud.utils.version.Version;
import lombok.Getter;
import lombok.Setter;

@ESObject
@Getter
@Setter
@JsonIgnoreProperties(value = {"alienManaged", "local"}, allowGetters = true)
public class GitLocation {

    public static final String DEPLOYMENT_CONFIG_PREFIX = "deployment_config_";

    public enum GitType {
        DeploymentConfig
    }

    @Id
    private String id;
    private String url;
    private GitType gitType;
    @JsonDeserialize(as = GitHardcodedCredential.class)
    private GitCredential credential;
    private String branch;
    private String path;

    // used by the UI side
    public boolean isAlienManaged() {
        return url.startsWith("file://");
    }

    public boolean isLocal() {
        return url.startsWith("file://");
    }

    public static class IdBuilder {

        private IdBuilder() {
        }

        public static class DeploymentConfig {
            public static String build(String environmentId) {
                return DEPLOYMENT_CONFIG_PREFIX + environmentId;
            }
        }
    }

    public static class IdExtractor {
        private IdExtractor() {
        }

        public static class DeploymentConfig {
            private DeploymentConfig() {
            }

            public static String extractEnvironmentId(String id) {
                return StringUtils.substringAfter(id, DEPLOYMENT_CONFIG_PREFIX);
            }
        }
    }
}
