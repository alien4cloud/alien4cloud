package org.alien4cloud.tosca.editor;

import java.util.Properties;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.processors.FileProcessorHelper;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.alien4cloud.tosca.variable.QuickFileStorageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import alien4cloud.component.repository.IFileRepository;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.application.EnvironmentType;
import alien4cloud.utils.TreeNode;

@Service
public class EditorFileService {
    @Inject
    private QuickFileStorageService quickFileStorageService;
    @Inject
    private IFileRepository artifactRepository;
    @Inject
    private EditionContextManager editionContextManager;

    /**
     * Load environment variables.
     * If the file is being edited (present in the edition context), then load that. If not found, then load from the persisted archive
     * 
     * @param environmentId
     * @return
     */
    public Properties loadEnvironmentVariables(String archiveId, String environmentId) {
        Supplier<Properties> persistedVarLoader = () -> quickFileStorageService.loadEnvironmentVariables(archiveId, environmentId, false);
        return execEnsuringContext(archiveId, () -> {
            if (EditionContextManager.get().getCsar().getId().equals(archiveId)) {
                return tryLoadVarsFromEditionContextFist(() -> quickFileStorageService.getRelativeEnvironmentVariablesFilePath(environmentId),
                        () -> loadVarsFromPersistedArchive(persistedVarLoader));
            }
            return loadVarsFromPersistedArchive(persistedVarLoader);
        });
    }

    /**
     * Load environment type variables.
     * If the file is being edited (present in the edition context), then load that. If not found, then load from the persisted archive
     *
     * @param environmentType
     * @return
     */
    public Properties loadEnvironmentTypeVariables(String archiveId, EnvironmentType environmentType) {
        Supplier<Properties> persistedVarLoader = () -> quickFileStorageService.loadEnvironmentTypeVariables(archiveId, environmentType, false);
        return execEnsuringContext(archiveId, () -> {
            if (EditionContextManager.get().getCsar().getId().equals(archiveId)) {
                return tryLoadVarsFromEditionContextFist(() -> quickFileStorageService.getRelativeEnvironmentTypeVariablesFilePath(environmentType.toString()),
                        () -> loadVarsFromPersistedArchive(persistedVarLoader));
            }
            return loadVarsFromPersistedArchive(persistedVarLoader);
        });
    }

    // private Properties tryLoadEnvironmentVarsFromEditionContextFistBis(String archiveId, String environmentId) {
    // try {
    // TreeNode varFileNode = FileProcessorHelper.getFileTreeNode(quickFileStorageService.getRelativeEnvironmentVariablesFilePath(environmentId));
    //
    // // the file is not being edited
    // if (StringUtils.isBlank(varFileNode.getArtifactId())) {
    // return loadEnvironmentVarsFromPersistedArchive(archiveId, environmentId);
    // } else {
    // // the file is being edited. Load the context one
    // Resource appVar = new PathResource(artifactRepository.resolveFile(varFileNode.getArtifactId()));
    // return PropertiesYamlParser.ToProperties.from(appVar);
    // }
    // } catch (NotFoundException e) {
    // // the file doesn't exists yet.
    // return new Properties();
    // }
    // }

    private Properties tryLoadVarsFromEditionContextFist(Supplier<String> pathSupplier, Supplier<Properties> persistedVariableLoaderSupplier) {
        try {
            TreeNode varFileNode = FileProcessorHelper.getFileTreeNode(pathSupplier.get());

            // the file is being edited. Load the context one
            if (StringUtils.isNotBlank(varFileNode.getArtifactId())) {
                Resource appVar = new PathResource(artifactRepository.resolveFile(varFileNode.getArtifactId()));
                return PropertiesYamlParser.ToProperties.from(appVar);
            } else {
                // the file is not being edited. Load from persisted files
                return persistedVariableLoaderSupplier.get();
            }
        } catch (NotFoundException e) {
            // the file doesn't exists yet.
            return new Properties();
        }
    }

    private Properties loadVarsFromPersistedArchive(Supplier<Properties> variablesLoaderSupplier) {
        Properties variables;
        variables = variablesLoaderSupplier.get();
        return variables;
    }

    private <T> T execEnsuringContext(String archiveId, Supplier<T> supplier) {
        boolean destroyContext = false;
        try {
            if (EditionContextManager.get() == null) {
                editionContextManager.init(archiveId);
                destroyContext = true;
            }
            return supplier.get();
        } finally {
            if (destroyContext) {
                editionContextManager.destroy();
            }
        }
    }
}
