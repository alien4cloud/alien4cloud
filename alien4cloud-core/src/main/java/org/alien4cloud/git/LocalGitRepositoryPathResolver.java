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

    private Path storageRootPath;
    private DeploymentConfigGitResolver deploymentConfigGitResolver = new DeploymentConfigGitResolver();

    public DeploymentConfigGitResolver forDeploymentConfig() {
        return deploymentConfigGitResolver;
    }

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

    public class DeploymentConfigGitResolver{
        private DeploymentConfigGitResolver(){}

        public Path resolveGitRoot(String environmentId) {
            return storageRootPath.resolve(environmentId);
        }
    }

    public Path findLocalPathRelatedToEnvironment(String environmentId) {
        return storageRootPath.resolve(environmentId);
    }

    @SneakyThrows
    public List<Path> findAllLocalDeploymentConfigGitPath() {
        return Files.walk(storageRootPath, 1).filter(path -> Files.isDirectory(path)).collect(Collectors.toList());
    }
}
