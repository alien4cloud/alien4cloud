package org.alien4cloud.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.CaseFormat;

import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;

@Component
public class LocalGitRepositoryPathResolver {
    private static final String ENV_DEPLOYMENT_SETUP_DIRECTORY = "env_setup";
    private static final String APP_VARIABLES_DIRECTORY = "vars";
    private Path storageRootPath;

    @Required
    @Value("${directories.alien}")
    public void setStorageRootPath(String rootDir) throws IOException {
        this.storageRootPath = FileUtil.createDirectoryIfNotExists(rootDir + "/git/");
    }

    /**
     * Return the "local path" where the AbstractDeploymentConfig file should be stored in the alien directory.
     *
     * @param clazz Implementation class of {@link AbstractDeploymentConfig}
     * @param deploymentConfigId deploymentConfigId of the AbstractDeploymentConfig
     * @param <T> the type of the {@link AbstractDeploymentConfig} implementation type
     * 
     * @return the path of the config file should be stored.
     */
    @SneakyThrows
    public <T extends AbstractDeploymentConfig> Path resolve(Class<T> clazz, String deploymentConfigId) {
        AbstractDeploymentConfig.VersionIdEnvId versionIdEnvId = AbstractDeploymentConfig.extractInfoFromId(deploymentConfigId);
        String fileName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName()) + ".yml";
        return storageRootPath.resolve(versionIdEnvId.getEnvironmentId()).resolve(fileName);
    }

    /**
     * Find the path in which application variables git repository is stored.
     * 
     * @param applicationId The id of the application for which to get the application variables path.
     * @return The application variables storage path.
     */
    public Path findApplicationVariableLocalPath(String applicationId) {
        return storageRootPath.resolve(applicationId).resolve(APP_VARIABLES_DIRECTORY);
    }

    /**
     * Find the path in which environment deployment setup git reporisoties are stored.
     * 
     * @param applicationId The id of the application the environment belongs to.
     * @param environmentId The id of the environment for which to get the path.
     * @return The path of the git repository for the given application environment.
     */
    public Path findEnvironmentSetupLocalPath(String applicationId, String environmentId) {
        return storageRootPath.resolve(applicationId).resolve(ENV_DEPLOYMENT_SETUP_DIRECTORY).resolve(environmentId);
    }

    @SneakyThrows
    public List<Path> findAllEnvironmentSetupLocalPath(String applicationId) {
        Path environmentSetupPath = storageRootPath.resolve(applicationId).resolve(ENV_DEPLOYMENT_SETUP_DIRECTORY);
        return Files.walk(environmentSetupPath, 1).filter(path -> !environmentSetupPath.equals(path) && Files.isDirectory(path)).collect(Collectors.toList());
    }
}