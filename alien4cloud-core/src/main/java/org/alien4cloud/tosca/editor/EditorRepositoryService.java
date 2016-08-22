package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import alien4cloud.git.RepositoryManager;
import alien4cloud.git.SimpleGitHistoryEntry;
import alien4cloud.security.model.User;
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
            log.debug("Initializing topology work directory at {}", localGitRepositoryPath.toAbsolutePath());
            Files.createDirectories(topologyGitPath);
        } else {
            log.debug("Topology work directory already created at {}", localGitRepositoryPath.toAbsolutePath());
        }
        if (!RepositoryManager.isGitRepository(topologyGitPath)) {
            RepositoryManager.create(topologyGitPath, "TOSCA topology created by Alien4Cloud.");
            log.debug("Initializing topology local git repository at {}", localGitRepositoryPath.toAbsolutePath());
        } else {
            log.debug("Topology local git repository already created at {}", localGitRepositoryPath.toAbsolutePath());
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

    public void commit(String topologyId, String message) {
        Path topologyGitPath = localGitRepositoryPath.resolve(topologyId);
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        String useremail = null;
        User user = auth == null ? null : (User) auth.getPrincipal();
        if (user != null) {
            username = user.getUsername();
            useremail = user.getEmail() == null ? username + "@undefined.org" : user.getEmail();
        }
        RepositoryManager.commitAll(topologyGitPath, username, useremail, message);
    }

    /**
     * Get the git history for a given topology.
     * 
     * @param topologyId The id of the topology.
     * @param from Start to query from the given history.
     * @param count The number of history entries to retrieve.
     * @return A list of simplified history entries.
     */
    public List<SimpleGitHistoryEntry> getHistory(String topologyId, int from, int count) {
        Path topologyGitPath = localGitRepositoryPath.resolve(topologyId);
        return RepositoryManager.getHistory(topologyGitPath, from, count);
    }
}