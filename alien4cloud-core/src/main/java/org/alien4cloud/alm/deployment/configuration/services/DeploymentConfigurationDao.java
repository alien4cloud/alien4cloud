package org.alien4cloud.alm.deployment.configuration.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.git.LocalGitManager;
import org.alien4cloud.git.LocalGitRepositoryPathResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import alien4cloud.utils.YamlParserUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DeploymentConfigurationDao {

    // TODO: add cache
    // TODO: add prefetch ?
    // TODO: eviction method?
    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;
    private LocalGitManager localGitManager;

    @Inject
    public DeploymentConfigurationDao(LocalGitManager localGitManager, LocalGitRepositoryPathResolver localGitRepositoryPathResolver) {
        this.localGitManager = localGitManager;
        this.localGitRepositoryPathResolver = localGitRepositoryPathResolver;
    }

    @SneakyThrows
    public <T extends AbstractDeploymentConfig> T findById(Class<T> clazz, String id) {
        Path path = localGitRepositoryPathResolver.resolve(clazz, id);
        T config = null;
        if(Files.exists(path)) {
            byte[] bytes = Files.readAllBytes(path);

            if (ArrayUtils.isNotEmpty(bytes)) {
                config = YamlParserUtil.parse(new String(bytes, StandardCharsets.UTF_8), clazz);
            }
        }
        return config;
    }

    @SneakyThrows
    public <T extends AbstractDeploymentConfig> void save(T deploymentInputs) {
        Date now = new Date();
        if (deploymentInputs.getCreationDate() == null) {
            deploymentInputs.setCreationDate(now);
        }
        deploymentInputs.setLastUpdateDate(now);

        Path path = localGitRepositoryPathResolver.resolve(deploymentInputs.getClass(), deploymentInputs.getId());
        String yaml = YamlParserUtil.toYaml(deploymentInputs);
        Files.createDirectories(path.getParent());
        Files.write(path, yaml.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void deleteAllByVersionId(String versionId) {
        List<Path> paths = localGitRepositoryPathResolver.findAllLocalPathRelatedToTopologyVersion(versionId);
        for (Path path : paths) {
            deleteDirectory(path);
        }
    }

    public void deleteAllByEnvironmentId(String environmentId) {
        Path path = localGitRepositoryPathResolver.findLocalPathRelatedToEnvironment(environmentId);
        deleteDirectory(path);
    }

    private void deleteDirectory(Path path) {
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            log.warn("Failed to delete <" + path + ">", e);
        }
    }

}
