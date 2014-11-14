package alien4cloud.it.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * IT Csar retriever is responsible for getting the CSAR artifacts that are required for automated IT tests.
 * 
 * Artifacts can be retrieved from git or related directories.
 */
@Slf4j
public class ITCsarRetriever {
    private Map<String[], String> remoteGitArtifacts = new HashMap<String[], String>();
    private Path artifactsDirectory = Paths.get("../target/it-artifacts");
    private boolean done;

    public ITCsarRetriever() {
        remoteGitArtifacts.put(new String[] { "https://github.com/alien4cloud/tosca-normative-types.git", "1.0.0.wd03" }, "tosca-normative-types-1.0.0.wd03");
        done = false;
    }

    public void retrieveGitArtifacts() {
        if (done) {
            return;
        }
        try {
            Files.createDirectories(artifactsDirectory);

            for (Entry<String[], String> remoteGitArtifact : remoteGitArtifacts.entrySet()) {
                Path targetPath = artifactsDirectory.resolve(remoteGitArtifact.getValue());

                if (Files.exists(targetPath)) {
                    Git.open(targetPath.toFile()).checkout();
                } else {
                    Files.createDirectories(targetPath);
                    cloneRepository(remoteGitArtifact.getKey()[0], remoteGitArtifact.getKey()[1], targetPath);
                }

            }
        } catch (IOException e) {
            log.error("Error while creating artifacts directory ", e);
        }
        done = true;
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