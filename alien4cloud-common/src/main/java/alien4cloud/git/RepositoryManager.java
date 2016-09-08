package alien4cloud.git;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.common.collect.Lists;

import alien4cloud.exception.GitException;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility to manage git repositories.
 */
@Slf4j
public class RepositoryManager {
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
            throw new GitException("Error while creating git repository ", e);
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
            throw new GitException("Unable to commit to the git repository ", e);
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
     *
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
            throw new GitException("Error while creating target directory ", e);
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
        if (username != null) {
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
        } catch (GitAPIException | IOException e) {
            throw new GitException("Unable to commit to the git repository ", e);
        } finally {
            close(repository);
        }
    }
}