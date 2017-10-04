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

import alien4cloud.git.RepositoryManager;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;

@Component
public class LocalGitRepositoryPathResolver {

    private Path storageRootPath;

    @Required
    @Value("${directories.alien}")
    public void setStorageRootPath(String rootDir) throws IOException {
        this.storageRootPath = FileUtil.createDirectoryIfNotExists(rootDir + "/fake_git/");
    }

    /**
     * Return the "local path" where the AbstractDeploymentConfig file should be stored in the alien directory.
     *
     * @param clazz Implementation class of {@link AbstractDeploymentConfig}
     * @param id id of the AbstractDeploymentConfig
     * @param <T> the type of the {@link AbstractDeploymentConfig} implementation type
     * 
     * @return the path of the config file should be stored.
     */
    @SneakyThrows
    public <T extends AbstractDeploymentConfig> Path resolve(Class<T> clazz, String id) {
        AbstractDeploymentConfig.VersionIdEnvId versionIdEnvId = AbstractDeploymentConfig.extractInfoFromId(id);
        String fileName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, clazz.getSimpleName()) + ".yml";

        Path localGitPath = storageRootPath.resolve(versionIdEnvId.getEnvironmentId());
        if (!Files.exists(localGitPath)) {
            RepositoryManager.create(localGitPath, null);
        }

        Path filePath = localGitPath.resolve(versionIdEnvId.getVersionId()).resolve(fileName);
        if (!Files.exists(filePath)) {
            Files.createDirectories(filePath.getParent());
            Files.createFile(filePath);
        }

        return filePath;
    }

    public Path resolveRootDir(String id) {
        AbstractDeploymentConfig.VersionIdEnvId versionIdEnvId = AbstractDeploymentConfig.extractInfoFromId(id);
        return storageRootPath.resolve(versionIdEnvId.getEnvironmentId());
    }

    /**
     * Return the directory that holds the configuration files related to this ID
     * 
     * @param id id of the AbstractDeploymentConfig
     * @return the directory where the configurations are stored
     */
    public Path resolveDirectory(String id) {
        AbstractDeploymentConfig.VersionIdEnvId versionIdEnvId = AbstractDeploymentConfig.extractInfoFromId(id);
        return storageRootPath.resolve(versionIdEnvId.getEnvironmentId()).resolve(versionIdEnvId.getVersionId());
    }

    public Path findLocalPathRelatedToEnvironment(String environmentId) {
        return storageRootPath.resolve(environmentId);
    }

    @SneakyThrows
    public List<Path> findAllLocalPathRelatedToTopologyVersion(String topologyVersion) {
        return Files.walk(storageRootPath, 2).filter(path -> {
            if (Files.isDirectory(path)) {
                // check if we its a level 1 directory (== envId)
                // or level 2 directory (== topologyVersion)
                if (!storageRootPath.equals(path.getParent())) {
                    return topologyVersion.equals(path.getFileName());
                }
            }

            return false;
        }).collect(Collectors.toList());
    }

}
