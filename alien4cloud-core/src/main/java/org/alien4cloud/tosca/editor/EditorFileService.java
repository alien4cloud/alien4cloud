package org.alien4cloud.tosca.editor;

import java.util.Map;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.alien4cloud.tosca.editor.processors.FileProcessorHelper;
import org.alien4cloud.tosca.utils.PropertiesYamlParser;
import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

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
     * Load inputs mapping.
     * If the file is being edited (present in the edition context), then load that. If not found, then load from the persisted archive.
     *
     * @param archiveId The id of the archive for which to load input mapping.
     * @return A Properties structure that contains the current defined input mappings.
     */
    public Map<String, Object> loadInputsVariables(String archiveId) {
        Supplier<Map<String, Object>> persistedVarLoader = () -> quickFileStorageService.loadInputsMappingFile(archiveId, false);
        return execEnsuringContext(archiveId,
                () -> tryLoadVarsFromEditionContextFist(() -> quickFileStorageService.getRelativeInputsFilePath(), persistedVarLoader));
    }

    /**
     * Load environment variables.
     * If the file is being edited (present in the edition context), then load that. If not found, then load from the persisted archive
     * 
     * @param archiveId The id of the archive for which to load variables.
     * @param environmentId The id of the environment for which to load variables.
     * @return A maps of variable expressions.
     */
    public Map<String, Object> loadEnvironmentVariables(String archiveId, String environmentId) {
        Supplier<Map<String, Object>> persistedVarLoader = () -> quickFileStorageService.loadEnvironmentVariablesAsMap(archiveId, environmentId, false);
        return execEnsuringContext(archiveId,
                () -> tryLoadVarsFromEditionContextFist(() -> quickFileStorageService.getRelativeEnvironmentVariablesFilePath(environmentId),
                        persistedVarLoader));
    }

    /**
     * Load environment type variables.
     * If the file is being edited (present in the edition context), then load that. If not found, then load from the persisted archive
     * 
     * @param archiveId The id of the archive for which to load variables.
     * @param environmentType The type of the environment for which to load variables.
     * @return A maps of variable expressions.
     */
    public Map<String, Object> loadEnvironmentTypeVariables(String archiveId, EnvironmentType environmentType) {
        Supplier<Map<String, Object>> persistedVarLoader = () -> quickFileStorageService.loadEnvironmentTypeVariablesAsMap(archiveId, environmentType, false);
        return execEnsuringContext(archiveId,
                () -> tryLoadVarsFromEditionContextFist(() -> quickFileStorageService.getRelativeEnvironmentTypeVariablesFilePath(environmentType.toString()),
                        persistedVarLoader));
    }

    private Map<String, Object> tryLoadVarsFromEditionContextFist(Supplier<String> pathSupplier,
            Supplier<Map<String, Object>> persistedVariableLoaderSupplier) {
        try {
            TreeNode varFileNode = FileProcessorHelper.getFileTreeNode(pathSupplier.get());

            // the file is being edited. Load the context one
            if (StringUtils.isNotBlank(varFileNode.getArtifactId())) {
                Resource appVar = new PathResource(artifactRepository.resolveFile(varFileNode.getArtifactId()));
                return PropertiesYamlParser.ToMap.from(appVar);
            } else {
                // the file is not being edited. Load from persisted files
                return persistedVariableLoaderSupplier.get();
            }
        } catch (NotFoundException e) {
            // the file doesn't exists yet.
            return Maps.newLinkedHashMap();
        }
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
