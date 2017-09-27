package org.alien4cloud.tosca.variable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.EditorRepositoryService;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;

import alien4cloud.model.application.EnvironmentType;
import alien4cloud.utils.FileUtil;
import lombok.SneakyThrows;

@Component
public class QuickFileStorageService {
    @Inject
    private EditorRepositoryService editorRepositoryService;

    private Path variablesStoreRootPath;

    @Required
    @Value("${directories.alien}")
    public void setStorageRootPath(String variableRootDir) throws IOException {
        this.variablesStoreRootPath = FileUtil.createDirectoryIfNotExists(variableRootDir + "/variables/");
    }

    public Properties loadApplicationVariables(String applicationId) {
        Path ymlPath = getApplicationVariablesPath(applicationId);
        return loadYamlToPropertiesIfExists(ymlPath);
    }

    public Properties loadEnvironmentTypeVariables(String archiveId, EnvironmentType environmentType) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, "inputs/" + sanitizeFilename("var_env_type_" + environmentType + ".yml"));
        return loadYamlToPropertiesIfExists(ymlPath);
    }

    public Properties loadEnvironmentVariables(String archiveId, String environmentId) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, "inputs/" + sanitizeFilename("var_env_" + environmentId + ".yml"));
        return loadYamlToPropertiesIfExists(ymlPath);
    }

    public Map<String, Object> loadInputsMappingFile(String archiveId) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, "inputs/inputs.yml");
        return loadYamlToMapIfExists(ymlPath);
    }

    @SneakyThrows
    private Properties loadYamlToPropertiesIfExists(Path ymlPath) {
        Properties props;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            props = PropertiesYamlParser.ToProperties.from(appVar);
        } else {
            Files.createDirectories(ymlPath.getParent());
            Files.createFile(ymlPath);
            props = new Properties();
        }

        return props;
    }

    @SneakyThrows
    private Map<String, Object> loadYamlToMapIfExists(Path ymlPath) {
        Map<String, Object> map;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            map = PropertiesYamlParser.ToMap.from(appVar);
        } else {
            Files.createDirectories(ymlPath.getParent());
            Files.createFile(ymlPath);
            map = Maps.newHashMap();
        }

        return map;
    }

    /**
     * Save application variables from a string content.
     *
     * @param applicationId The id of the application.
     * @param data The content of the variable file.
     */
    @SneakyThrows
    public void saveApplicationVariables(String applicationId, InputStream data) {
        Path ymlPath = getApplicationVariablesPath(applicationId);
        Files.copy(data, ymlPath, StandardCopyOption.REPLACE_EXISTING);
    }

    @SneakyThrows
    public String getApplicationVariables(String applicationId) {
        Path ymlPath = getApplicationVariablesPath(applicationId);
        if (Files.exists(ymlPath)) {
            return FileUtil.readTextFile(ymlPath);
        }
        return "";
    }

    private Path getApplicationVariablesPath(String applicationId) {
        return this.variablesStoreRootPath.resolve(sanitizeFilename("app_" + applicationId + ".yml"));
    }

    private String sanitizeFilename(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
}
