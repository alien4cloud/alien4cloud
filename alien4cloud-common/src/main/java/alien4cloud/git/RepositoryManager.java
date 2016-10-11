package alien4cloud.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.DeleteBranchCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RenameBranchCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.collect.Lists;

import alien4cloud.exception.GitConflictException;
import alien4cloud.exception.GitException;
import alien4cloud.exception.GitMergingStateException;
import alien4cloud.exception.GitStateException;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to manage git repositories.
 */
@Slf4j
public class RepositoryManager {

    private static final String REMOTE_ALIEN_CONFLICTS_PREFIX_BRANCH_NAME = "alien-conflicts-";

    /**
     * Close a repository.
     *
     * @param repository The repository to close.
     */
    public static void close(Git repository) {
        if (repository != null) {
            repository.close();
        }
    }

    /**
     * Check if a given directory is a git repository.
     *
     * @param targetDirectory The directory to check.
     * @return true if the directory is a git repository, false if not.
     */
    public static boolean isGitRepository(Path targetDirectory) {
        Git repository = null;
        try {
            repository = Git.open(targetDirectory.toFile());
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            close(repository);
        }
    }

    /**
     * Create a git repository that includes an optional readme file.
     *
     * @param targetDirectory The path of the repository to create.
     * @param readmeContentIfEmpty
     */
    public static void create(Path targetDirectory, String readmeContentIfEmpty) {
        Git repository = null;
        try {
            repository = Git.init().setDirectory(targetDirectory.toFile()).call();
            if (readmeContentIfEmpty != null) {
                Path readmePath = targetDirectory.resolve("readme.txt");
                File file = readmePath.toFile();
                file.createNewFile();
                try (BufferedWriter writer = Files.newBufferedWriter(readmePath)) {
                    writer.write(readmeContentIfEmpty);
                }
            }
        } catch (GitAPIException | IOException e) {
            throw new GitException("Error while creating git repository", e);
        } finally {
            close(repository);
        }
    }

    /**
     * Commit all changes in the given repository.
     *
     * @param targetDirectory The target directory.
     */
    public static void commitAll(Path targetDirectory, String userName, String userEmail, String commitMessage) {
        Git repository = null;
        try {
            repository = Git.open(targetDirectory.toFile());
            repository.add().addFilepattern(".").call();
            repository.commit().setCommitter(userName, userEmail).setMessage(commitMessage).call();
        } catch (GitAPIException | IOException e) {
            throw new GitException("Unable to commit to the git repository", e);
        } finally {
            close(repository);
        }
    }

    /**
     * Clone or checkout a git repository in a local directory relative to the given targetDirectory.
     *
     * @param targetDirectory The root directory that will contains the localDirectory in which to checkout the archives.
     * @param repositoryUrl The url of the repository to checkout or clone.
     * @param branch The branch to checkout or clone.
     * @param localDirectory The path, relative to targetDirectory, in which to checkout or clone the git directory.
     */
    public static void cloneOrCheckout(Path targetDirectory, String repositoryUrl, String branch, String localDirectory) {
        Git repository = null;
        try {
            repository = cloneOrCheckout(targetDirectory, repositoryUrl, null, null, branch, localDirectory);
        } finally {
            close(repository);
        }
    }

    /**
     * @param targetDirectory The root directory that will contains the localDirectory in which to checkout the archives.
     * @param repositoryUrl The url of the repository to checkout or clone.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     * @param branch The branch to checkout or clone.
     * @param localDirectory The path, relative to targetDirectory, in which to checkout or clone the git directory.
     */
    public static Git cloneOrCheckout(Path targetDirectory, String repositoryUrl, String username, String password, String branch, String localDirectory) {
        try {
            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(localDirectory);
            Git repository;
            if (Files.exists(targetPath)) {
                try {
                    repository = Git.open(targetPath.toFile());
                    checkoutRepository(repository, branch, username, password);
                } catch (RepositoryNotFoundException e) {
                    // TODO delete the folder
                    FileUtil.delete(targetPath);
                    repository = cloneRepository(repositoryUrl, username, password, branch, targetPath);
                }
            } else {
                Files.createDirectories(targetPath);
                repository = cloneRepository(repositoryUrl, username, password, branch, targetPath);
            }
            return repository;
        } catch (IOException e) {
            throw new GitException("Error while creating target directory", e);
        }
    }

    private static void checkoutRepository(Git repository, String branch, String username, String password) {
        try {
            CheckoutCommand checkoutCommand = repository.checkout();
            // had to add "origin/" to fix an error when trying to checkout a branch
            checkoutCommand.setName("origin/" + branch);
            Ref ref = checkoutCommand.call();
            if (ref == null || branch.equals(ref.getName())) {
                // failed to checkout the branch, let's fetch it
                // TODO: this part seems useless. check it out
                FetchCommand fetchCommand = repository.fetch();
                setCredentials(fetchCommand, username, password);
                fetchCommand.call();
                checkoutCommand = repository.checkout();
                checkoutCommand.setName("origin/" + branch);
                checkoutCommand.call();
            }
        } catch (GitAPIException e) {
            throw new GitException("Failed to pull git repository", e);
        }
    }

    private static Git cloneRepository(String url, String username, String password, String branch, Path targetPath) throws IOException {
        log.debug("Cloning from [{}] branch [{}] to [{}]", url, branch, targetPath.toString());
        Git result;
        try {
            CloneCommand cloneCommand = Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(targetPath.toFile());
            setCredentials(cloneCommand, username, password);
            result = cloneCommand.call();
            log.debug("Cloned repository to [{}]: ", result.getRepository().getDirectory());
            return result;
        } catch (GitAPIException e) {
            // if the import fails then we should try to remove the created directory
            FileUtil.delete(targetPath);
            throw new GitException("Failed to clone git repository", e);
        }
    }

    /**
     * Trigger a Pull Request on an existing repository.
     *
     * @param repository The git repository to pull.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     * @return True if the pull request has updated the data, false if the repository was already up to date.
     */
    public static boolean pull(Git repository, String username, String password) {
        try {
            PullCommand pullCommand = repository.pull();
            setCredentials(pullCommand, username, password);
            PullResult result = pullCommand.call();

            MergeResult mergeResult = result.getMergeResult();
            if (mergeResult != null && MergeResult.MergeStatus.ALREADY_UP_TO_DATE == mergeResult.getMergeStatus()) {
                return false; // nothing has changed
            }
            return true;
        } catch (GitAPIException e) {
            throw new GitException("Failed to pull git repository", e);
        }
    }

    /**
     * Get the hash of the last commit on the current branch of the given repository.
     *
     * @param git The repository from which to get the last commit hash.
     * @return The hash of the last commit.
     */
    public static String getLastHash(Git git) {
        try {
            Iterator<RevCommit> revCommitIterator = git.log().setMaxCount(1).call().iterator();
            if (revCommitIterator.hasNext()) {
                return revCommitIterator.next().getName();
            }
        } catch (GitAPIException e) {
            throw new GitException("Failed to log git repository", e);
        }
        return null;
    }

    private static void setCredentials(TransportCommand<?, ?> command, String username, String password) {
        if (StringUtils.isNotBlank(username)) {
            if (password == null) {
                // If an user accessing a GitHub repository through HTTPS with an OAuth access token
                password = "";
            }
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        }
    }

    /**
     * Return a simplified git commit history list.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param from Start to query from the given history.
     * @param count The number of history entries to retrieve.
     * @return A list of simplified history entries.
     */
    public static List<SimpleGitHistoryEntry> getHistory(Path repositoryDirectory, int from, int count) {
        Git repository = null;
        try {
            repository = Git.open(repositoryDirectory.toFile());
            Iterable<RevCommit> commits = repository.log().setSkip(from).setMaxCount(count).call();
            List<SimpleGitHistoryEntry> historyEntries = Lists.newArrayList();
            for (RevCommit commit : commits) {
                historyEntries.add(new SimpleGitHistoryEntry(commit.getId().getName(), commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getEmailAddress(), commit.getFullMessage(), new Date(commit.getCommitTime() * 1000L)));
            }
            return historyEntries;
        } catch (NoHeadException e) {
            log.debug("Your repository has no head, you need to save your topology before using the git history.");
            return Lists.newArrayList();
        } catch (GitAPIException | IOException e) {
            throw new GitException("Unable to get history from the git repository", e);
        } finally {
            close(repository);
        }
    }

    /**
     * Set a remote repository path.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param remoteName The name of the remote (i.e. 'origin')
     * @param remoteUrl The url of the repository.
     */
    public static void setRemote(Path repositoryDirectory, String remoteName, String remoteUrl) {
        Git git = null;
        try {
            git = Git.open(repositoryDirectory.toFile());
            StoredConfig config = git.getRepository().getConfig();
            config.unsetSection("remote", remoteName);
            RemoteConfig remoteConfig = new RemoteConfig(config, remoteName);
            remoteConfig.addURI(new URIish(remoteUrl));
            remoteConfig.addFetchRefSpec(new RefSpec("+refs/heads/*:refs/remotes/" + remoteName + "/*"));
            remoteConfig.update(config);
            config.save();
        } catch (URISyntaxException | IOException e) {
            throw new GitException("Unable to set the remote repository", e);
        } finally {
            close(git);
        }
    }

    /**
     * Get the URL of the git remote.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param remoteName The name of the remote
     * @return The url of the git remote.
     */
    public static String getRemoteUrl(Path repositoryDirectory, String remoteName) {
        Git git = null;
        try {
            git = Git.open(repositoryDirectory.toFile());
            return git.getRepository().getConfig().getString("remote", remoteName, "url");
        } catch (IOException e) {
            throw new GitException("Unable to open the git repository", e);
        } finally {
            close(git);
        }
    }

    /**
     * Git push to a remote.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     * @return Returns <code>true</code> pushed, <code>false</code> otherwise.
     */
    public static boolean push(Path repositoryDirectory, String username, String password) {
        return push(repositoryDirectory, username, password, null);
    }

    /**
     * Git push to a remote.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     * @return <code>true</code> pushed, <code>false</code> otherwise.
     */
    public static boolean push(Path repositoryDirectory, String username, String password, String remoteBranch) {
        Git git = null;
        try {
            git = Git.open(repositoryDirectory.toFile());
            checkRepositoryState(git.getRepository().getRepositoryState(), "Git push operation failed.");
            Repository repository = git.getRepository();

            // If no given remoteBranch, use the default one (i.e. master).
            String targetRemoteBranch = remoteBranch == null ? repository.getBranch() : remoteBranch;
            boolean isPushed = push(git, username, password, repository.getBranch(), targetRemoteBranch);
            if (!isPushed) {
                // If not pushed, then we have a conflict.
                // Push the current commit into a new alien branch.
                // Then rebranch to the current branch.
                String remoteName = repository.getRemoteNames().iterator().next(); // Only handle one remote (default: 'origin')
                log.debug(String.format("Couldn't push git repository=%s to remote=%s on the branch=%s", git.getRepository().getDirectory(), remoteName,
                        repository.getBranch()));
                fetch(git, username, password);
                String conflictBranchName = generateConflictBranchName(repository, remoteName);
                isPushed = push(git, username, password, repository.getBranch(), conflictBranchName);
                if (isPushed) {
                    log.debug(String.format("Pushed git repository=%s on branch=%s", git.getRepository().getDirectory(), conflictBranchName));
                    rebranch(git, repository.getBranch(), targetRemoteBranch);
                }
                throw new GitConflictException(remoteName, repository.getBranch(), conflictBranchName);
            } else {
                log.debug(String.format("Pushed git repository=%s on branch=%s", git.getRepository().getDirectory(), targetRemoteBranch));
            }
            return isPushed;
        } catch (IOException e) {
            throw new GitException("Unable to open the remote repository", e);
        } finally {
            close(git);
        }
    }

    /**
     * Generate the conflict branch name to push to.
     */
    private static String generateConflictBranchName(Repository repository, String remoteName) {
        Map<String, Ref> allRefs = repository.getAllRefs();
        String remoteAlienRefSpecPrefixName = String.format("refs/remotes/%s/%s", remoteName, REMOTE_ALIEN_CONFLICTS_PREFIX_BRANCH_NAME);
        long count = allRefs.keySet().stream().filter(key -> key.startsWith(remoteAlienRefSpecPrefixName)).count();
        return String.format("%s%d-%d", REMOTE_ALIEN_CONFLICTS_PREFIX_BRANCH_NAME, new Date().getTime(), count + 1);
    }

    /**
     * Rebranch the local and remote branch.
     *
     * @param git The git repository.
     * @param localBranch The name of the local branch.
     * @param remoteBranch The name of the remote branch.
     */
    public static void rebranch(Git git, String localBranch, String remoteBranch) {
        String tmpBranchName = "a4c-switch";
        try {
            log.debug(String.format("Prepare git repository=%s to re-branch=%s on remote branch=%s", git.getRepository().getDirectory(), localBranch,
                    remoteBranch));
            CheckoutCommand checkoutCommand = git.checkout();
            checkoutCommand.setStartPoint("origin/" + remoteBranch);
            checkoutCommand.setName(tmpBranchName);
            checkoutCommand.setCreateBranch(true);
            checkoutCommand.call();
            log.debug(String.format("Delete branch=%s from git repository=%s", localBranch, git.getRepository().getDirectory()));
            DeleteBranchCommand deleteBranchCommand = git.branchDelete();
            deleteBranchCommand.setBranchNames(localBranch);
            deleteBranchCommand.setForce(true);
            deleteBranchCommand.call();
            log.debug(String.format("Finalize git re-branch=%s for repository=%s", localBranch, git.getRepository().getDirectory()));
            RenameBranchCommand renameBranchCommand = git.branchRename();
            renameBranchCommand.setOldName(tmpBranchName);
            renameBranchCommand.setNewName(localBranch);
            renameBranchCommand.call();
        } catch (GitAPIException e) {
            throw new GitException("Couldn't rebranch to origin common branch", e);
        }
    }

    /**
     * Git push to a remote.
     *
     * @param git The git repository.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     * @param localBranch The name of the local branch to push.
     * @param remoteBranch The name of the remote branch to push to.
     * @return <code>true</code> pushed, <code>false</code> otherwise.
     */
    public static boolean push(Git git, String username, String password, String localBranch, String remoteBranch) {
        try {
            if (git.getRepository().getRemoteNames().isEmpty()) {
                throw new GitException("No remote found for the repository");
            }
            PushCommand pushCommand = git.push();
            setCredentials(pushCommand, username, password);
            RefSpec refSpec = new RefSpec(String.format("refs/heads/%s:refs/heads/%s", localBranch, remoteBranch));
            pushCommand.setRefSpecs(refSpec);
            Iterable<PushResult> call = pushCommand.call();
            return isPushed(call);
        } catch (GitAPIException e) {
            throw new GitException(String.format("Error when trying to git push: %s", e.getMessage()), e);
        }
    }

    /**
     * Inspect the push returns to know if the push command has succeeded.
     *
     * @param call The push results object.
     * @return Returns <code>true</code> pushed, <code>false</code> otherwise.
     */
    private static boolean isPushed(Iterable<PushResult> call) {
        for (PushResult pr : call) {
            Optional<RemoteRefUpdate> any = pr.getRemoteUpdates().stream()
                    .filter(ru -> !(RemoteRefUpdate.Status.OK.equals(ru.getStatus()) || RemoteRefUpdate.Status.UP_TO_DATE.equals(ru.getStatus()))).findAny();
            if (any.isPresent()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fetch a git repository.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     */
    public static void fetch(Path repositoryDirectory, String username, String password) {
        Git repository = null;
        try {
            repository = Git.open(repositoryDirectory.resolve(".git").toFile());
            fetch(repository, username, password);
        } catch (IOException e) {
            throw new GitException("Unable to open the remote repository", e);
        } finally {
            close(repository);
        }
    }

    /**
     * Fetch a git repository.
     *
     * @param git The git repository.
     * @param username The username to use for the repository connection.
     * @param password The password to use for the repository connection.
     */
    public static void fetch(Git git, String username, String password) {
        try {
            FetchCommand fetchCommand = git.fetch();
            setCredentials(fetchCommand, username, password);
            FetchResult fetchResult = fetchCommand.call();
            log.debug(String.format("Fetched git repository=%s messages=%s", git.getRepository().getDirectory(), fetchResult.getMessages()));
        } catch (GitAPIException e) {
            throw new GitException("Unable to fetch git repository", e);
        }
    }

    /**
     * Pull modifications from the default branch a git repository.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param username The username for the git repository connection, null if none.
     * @param password The password for the git repository connection, null if none.
     */
    public static void pull(Path repositoryDirectory, String username, String password) {
        pull(repositoryDirectory, username, password, null);
    }

    /**
     * Pull modifications a git repository.
     *
     * @param repositoryDirectory The directory in which the git repo exists.
     * @param username The username for the git repository connection, null if none.
     * @param password The password for the git repository connection, null if none.
     * @param remoteBranch The name of the remote branch to pull from.
     */
    public static void pull(Path repositoryDirectory, String username, String password, String remoteBranch) {
        Git git = null;
        try {
            git = Git.open(repositoryDirectory.resolve(".git").toFile());
            if (git.getRepository().getRemoteNames().isEmpty()) {
                throw new GitException("No remote found for the repository");
            }
            checkRepositoryState(git.getRepository().getRepositoryState(), "Git pull operation failed");
            PullCommand pullCommand = git.pull();
            setCredentials(pullCommand, username, password);
            pullCommand.setRemoteBranchName(remoteBranch);
            PullResult call = pullCommand.call();
            if (call.getMergeResult() != null && call.getMergeResult().getConflicts() != null && !call.getMergeResult().getConflicts().isEmpty()) {
                throw new GitConflictException(git.getRepository().getBranch());
            }
            log.debug(String.format("Successfully pulled from %s", call.getFetchedFrom()));
        } catch (IOException e) {
            throw new GitException("Unable to open the git repository", e);
        } catch (GitAPIException e) {
            throw new GitException("Unable to pull the git repository", e);
        } finally {
            close(git);
        }
    }

    /**
     * Check the given state of a git repository.
     * This method throws exceptions if the state is not SAFE.
     *
     * @param repositoryState The state of the repository.
     * @param errorMessage A message error.
     */
    private static void checkRepositoryState(RepositoryState repositoryState, String errorMessage) {
        switch (repositoryState) {
        case SAFE:
            return;
        case MERGING:
            throw new GitMergingStateException(errorMessage);
        default:
            throw new GitStateException(errorMessage, repositoryState.toString());
        }
    }
}