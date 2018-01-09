package org.alien4cloud.git.model;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.annotation.ESObject;
import org.elasticsearch.annotation.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ESObject
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(value = { "alienManaged", "local" }, allowGetters = true)
public class GitLocation {
    public static final String ID_SEPARATOR = "::";
    public static final String LOCAL_PREFIX = "file://";
    public static final String APPLICATION_VARIABLES_PREFIX = "app_vars_";
    public static final String DEPLOYMENT_CONFIG_PREFIX = "deployment_config_";

    public enum GitType {
        DeploymentConfig, ApplicationVariables
    }

    @Id
    private String id;
    private String url;
    private GitType gitType;
    @JsonDeserialize(as = GitHardcodedCredential.class)
    private GitCredential credential;
    private String branch;
    private String path;

    public boolean isLocal() {
        return url.startsWith(LOCAL_PREFIX);
    }

    public String usernameOrNull() {
        return credential == null ? null : credential.getUsername();
    }

    public String passwordOrNull() {
        return credential == null ? null : credential.getPassword();
    }

    public static class IdBuilder {

        private IdBuilder() {
        }

        public static String forApplicationVariables(String applicationId) {
            return APPLICATION_VARIABLES_PREFIX + applicationId;
        }

        public static String forDeploymentSetup(String applicationId, String environmentId) {
            return DEPLOYMENT_CONFIG_PREFIX + applicationId + ID_SEPARATOR + environmentId;
        }
    }

    public static class IdExtractor {
        private IdExtractor() {
        }

        public static String fromApplicationVariables(String id) {
            return StringUtils.substringAfter(id, APPLICATION_VARIABLES_PREFIX);
        }

        public static AppEnvIds fromDeploymentSetup(String id) {
            return new AppEnvIds(StringUtils.substringAfter(id, DEPLOYMENT_CONFIG_PREFIX));
        }
    }

    @Getter
    @Setter
    public static class AppEnvIds {
        private String applicationId;
        private String environmentId;

        public AppEnvIds(String id) {
            String[] splitted = id.split(ID_SEPARATOR);
            applicationId = splitted[0];
            applicationId = splitted[1];
        }
    }
}
