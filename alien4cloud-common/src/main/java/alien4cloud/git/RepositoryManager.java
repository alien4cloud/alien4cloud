package alien4cloud.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Utility to manage git repositories.
 */
@Slf4j
public class RepositoryManager {

    public void cloneOrCheckout(Path targetDirectory, String repositoryUrl, String branch, String localDirectory) {
        try {
            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(localDirectory);

            if (Files.exists(targetPath)) {
                Git.open(targetPath.toFile()).checkout();
            } else {
                Files.createDirectories(targetPath);
                cloneRepository(repositoryUrl, branch, targetPath);
            }

        } catch (IOException e) {
            log.error("Error while creating target directory ", e);
        }
    }

    private void cloneRepository(String url, String branch, Path targetPath) {
        // then clone
        log.info("Cloning from [" + url + "] branch [" + branch + "] to [" + targetPath.toString() + "]");
        Git result;
        try {
            result = Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(targetPath.toFile()).call();
            try {
                // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
                log.info("Cloned: " + result.getRepository().getDirectory());
            } finally {
                result.close();
            }
        } catch (GitAPIException e) {
            log.error("Failed to clone git repository.", e);
        }
    }
}