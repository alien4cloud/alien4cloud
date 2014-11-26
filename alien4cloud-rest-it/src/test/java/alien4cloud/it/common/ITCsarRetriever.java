package alien4cloud.it.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;
import alien4cloud.git.RepositoryManager;

/**
 * IT Csar retriever is responsible for getting the CSAR artifacts that are required for automated IT tests.
 * 
 * Artifacts can be retrieved from git or related directories.
 */
@Slf4j
public class ITCsarRetriever {
    private RepositoryManager repositoryManager = new RepositoryManager();
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
                repositoryManager.cloneOrCheckout(artifactsDirectory, remoteGitArtifact.getKey()[0], remoteGitArtifact.getKey()[1],
                        remoteGitArtifact.getValue());
            }
        } catch (IOException e) {
            log.error("Error while creating artifacts directory ", e);
        }
        done = true;
    }
}