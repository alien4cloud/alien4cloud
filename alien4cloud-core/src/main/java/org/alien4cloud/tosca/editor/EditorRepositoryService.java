package org.alien4cloud.tosca.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.repository.ICsarRepositry;
import org.alien4cloud.tosca.model.Csar;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import alien4cloud.git.RepositoryManager;
import alien4cloud.git.SimpleGitHistoryEntry;
import alien4cloud.security.model.User;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage git repository integration for the editor.
 *
 * For every archive (bound to a CSAR or to an Application)
 */
@Slf4j
@Component
public class EditorRepositoryService {
    @Inject
    private ICsarRepositry csarRepositry;
    // TODO we need a Git Branch Strategy Handler as well as a Git Multi-directory Handler so we can handle edition of repositories

    public Path resolveArtifact(String csarId, String artifactReference) {
        // let just split archiveName, archiveVersion, archiveWorkspace
        String[] splittedId = csarId.split(":");
        return csarRepositry.getExpandedCSAR(splittedId[0], splittedId[1]);
    }

    /**
     * Create a local git repository for the given topology.
     *
     * @param csar The archive for which to ensure the git repository is initialized.
     * @return The path that contains the archive git repository.
     * @throws IOException
     */
    public Path createGitDirectory(Csar csar) throws IOException {
        // FIXME we should use directly the folder from the archive repository
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        if (!RepositoryManager.isGitRepository(archiveGitPath)) {
            RepositoryManager.create(archiveGitPath, "TOSCA topology created by Alien4Cloud.");
            log.debug("Initializing topology local git repository at {}", archiveGitPath.toAbsolutePath());
        } else {
            log.debug("Topology local git repository already created at {}", archiveGitPath.toAbsolutePath());
        }
        return archiveGitPath;
    }

    /**
     * Commit the changes to the repository using the given message as commit message.
     * 
     * @param csar The csar under edition for which to commit changes.
     * @param message The message to use for the commit.
     */
    public void commit(Csar csar, String message) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = null;
        String useremail = null;
        User user = auth == null ? null : (User) auth.getPrincipal();
        if (user != null) {
            username = user.getUsername();
            useremail = user.getEmail() == null ? username + "@undefined.org" : user.getEmail();
        }
        RepositoryManager.commitAll(archiveGitPath, username, useremail, message);
    }

    /**
     * Get the git history for a given archive.
     * 
     * @param csar The archive under edition.
     * @param from Start to query from the given history.
     * @param count The number of history entries to retrieve.
     * @return A list of simplified history entries.
     */
    public List<SimpleGitHistoryEntry> getHistory(Csar csar, int from, int count) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        return RepositoryManager.getHistory(archiveGitPath, from, count);
    }

    /**
     * Set a remote repository.
     *
     * @param csar The archive for which to set the remote.
     * @param remoteName The remote name.
     * @param remoteUrl The repository url.
     */
    public void setRemote(Csar csar, String remoteName, String remoteUrl) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        RepositoryManager.setRemote(archiveGitPath, remoteName, remoteUrl);
    }

    /**
     * Get the url of the remote git repository.
     *
     * @param csar The concerned archive.
     * @param remoteName The name of the remote
     * @return The url corresponding to the remote name.
     */
    public String getRemoteUrl(Csar csar, String remoteName) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        return RepositoryManager.getRemoteUrl(archiveGitPath, remoteName);
    }

    /**
     * Push modifications to git repository.
     *
     * @param csar The concerned archive.
     * @param username The username of the git repository, null if none.
     * @param password The password of the git repository, null if none.
     * @param remoteBranch The name of the remote branch to push to.
     */
    public void push(Csar csar, String username, String password, String remoteBranch) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        RepositoryManager.push(archiveGitPath, username, password, remoteBranch);
    }

    /**
     * Pull modifications from the git repository.
     *
     * @param csar The concerned archive.
     * @param username The username of the git repository, null if none.
     * @param password The password of the git repository, null if none.
     * @param remoteBranch The name of the remote branch to pull from.
     */
    public void pull(Csar csar, String username, String password, String remoteBranch) {
        Path archiveGitPath = csarRepositry.getExpandedCSAR(csar.getName(), csar.getVersion());
        RepositoryManager.pull(archiveGitPath, username, password, remoteBranch);
    }
}