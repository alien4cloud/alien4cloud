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
        return loadYamlToPropertiesIfExists(ymlPath, true);
    }

    public Properties loadEnvironmentTypeVariables(String archiveId, EnvironmentType environmentType) {
        return loadEnvironmentTypeVariables(archiveId, environmentType, true);
    }

    public Properties loadEnvironmentTypeVariables(String archiveId, EnvironmentType environmentType, boolean createIfFileNotExists) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, getRelativeEnvironmentTypeVariablesFilePath(environmentType.toString()));
        return loadYamlToPropertiesIfExists(ymlPath, createIfFileNotExists);
    }

    public Map<String, Object> loadEnvironmentTypeVariablesAsMap(String archiveId, EnvironmentType environmentType, boolean createIfFileNotExists) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, getRelativeEnvironmentTypeVariablesFilePath(environmentType.toString()));
        return loadYamlToMapIfExists(ymlPath, createIfFileNotExists);
    }

    public Properties loadEnvironmentVariables(String archiveId, String environmentId) {
        return loadEnvironmentVariables(archiveId, environmentId, true);
    }

    public Properties loadEnvironmentVariables(String archiveId, String environmentId, boolean createIfFileNotExists) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, getRelativeEnvironmentVariablesFilePath(environmentId));
        return loadYamlToPropertiesIfExists(ymlPath, createIfFileNotExists);
    }

    public Map<String, Object> loadEnvironmentVariablesAsMap(String archiveId, String environmentId, boolean createIfFileNotExists) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, getRelativeEnvironmentVariablesFilePath(environmentId));
        return loadYamlToMapIfExists(ymlPath, createIfFileNotExists);
    }

    public Map<String, Object> loadInputsMappingFile(String archiveId) {
        return loadInputsMappingFile(archiveId);
    }

    public Map<String, Object> loadInputsMappingFile(String archiveId, boolean createIfFileNotExists) {
        Path ymlPath = editorRepositoryService.resolveArtifact(archiveId, getRelativeInputsFilePath());
        return loadYamlToMapIfExists(ymlPath, createIfFileNotExists);
    }

    @SneakyThrows
    private Properties loadYamlToPropertiesIfExists(Path ymlPath, boolean createFileIfNotExists) {
        Properties props;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            props = PropertiesYamlParser.ToProperties.from(appVar);
        } else {
            if (createFileIfNotExists) {
                Files.createDirectories(ymlPath.getParent());
                Files.createFile(ymlPath);
            }
            props = new Properties();
        }

        return props;
    }

    @SneakyThrows
    private Map<String, Object> loadYamlToMapIfExists(Path ymlPath, boolean createFileIfNotExists) {
        Map<String, Object> map;
        if (Files.exists(ymlPath)) {
            Resource appVar = new PathResource(ymlPath);
            map = PropertiesYamlParser.ToMap.from(appVar);
        } else {
            if (createFileIfNotExists) {
                Files.createDirectories(ymlPath.getParent());
                Files.createFile(ymlPath);
            }
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

    public String getRelativeEnvironmentVariablesFilePath(String environmentId) {
        return "inputs/" + sanitizeFilename("var_env_" + environmentId + ".yml");
    }

    public String getRelativeEnvironmentTypeVariablesFilePath(String environmentType) {
        return "inputs/" + sanitizeFilename("var_env_type_" + environmentType + ".yml");
    }

    public String getRelativeInputsFilePath() {
        return "inputs/inputs.yml";
    }
}
