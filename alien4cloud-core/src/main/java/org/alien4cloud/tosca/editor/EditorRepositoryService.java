package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage git repository integration for the editor.
 *
 * For every archive (bound to a CSAR or to an Application)
 */
@Slf4j
@Component
public class EditorRepositoryService {
    private Path localGitRepositoryPath;

    // TODO we need a Git Branch Strategy Handler as well as a Git Multi-directory Handler so we can handle edition of repositories

    @Value("${directories.alien}")
    public void setLocalGitRepositoryPath(String pathStr) {
        localGitRepositoryPath = Paths.get(pathStr).resolve("editor");
    }

    /**
     * Create a local git repository for the given topology.
     *
     * @param topologyId The id of the topology for which to create the git repository.
     * @return
     * @throws IOException
     */
    public Path createGitDirectory(String topologyId) throws IOException {
        Path topologyGitPath = localGitRepositoryPath.resolve(topologyId);
        if (!Files.isDirectory(topologyGitPath)) {
            log.debug("Initializing topology git repository {}", localGitRepositoryPath.toAbsolutePath());
            Files.createDirectories(topologyGitPath);
        } else {
            log.debug("Alien Repository folder already created at {}", localGitRepositoryPath.toAbsolutePath());
        }
        return topologyGitPath;
    }

    /**
     * Copy topology archive files from the given location.
     * 
     * @param topologyId The id of the topology under edition.
     * @param source The source path.
     */
    public void copyFrom(String topologyId, Path source) throws IOException {
        // FIXME Manage Git Strategy handler as this can source can already be a git repo (or a folder in a git repo)
        // check that directory is a git repo, if not initialize it and commit as initial data
        Path topologyGitPath = localGitRepositoryPath.resolve(topologyId);
        FileUtil.copy(source, topologyGitPath);
    }
}
