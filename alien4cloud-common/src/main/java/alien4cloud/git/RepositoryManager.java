package alien4cloud.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.springframework.beans.factory.annotation.Value;

import alien4cloud.exception.GitCloneUriException;
import alien4cloud.utils.FileUtil;

/**
 * Utility to manage git repositories.
 */
@Slf4j
public class RepositoryManager {
    @Getter
    private Map<String, String> locations = new HashMap<String, String>();
    @Getter
    private ArrayList<Path> csarsToImport = new ArrayList<Path>();
    @Getter
    private Path pathToReach;
    @Value("${directories.alien}/${directories.upload_temp}")
    private String alienTempUpload;
    public static String _SUFFIXE = "_ZIPPED";
    public static String _ALL = "_ALL";
    public static String _DEFAULTSEPARATOR = "/";

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

    /**
     * Clone or checkout a GitHub Repository and its sub-branch contained in a Map<Url,Branch>
     * 
     * @param alienTpmPath The folder to clone the repositories
     * @param repositoryUrl The GitHub url to reach the repository
     * @param branchMap A Map of the sub-repositories with its branchId (i.e : master,develop etc)
     * @param localDirectory Static path to be resolved with targetDirectory
     * @throws GitCloneUriException 
     * @throws GitAPIException 
     */
    public String createFolderAndClone(Path alienTpmPath, String repositoryUrl, Map<String, String> branchMap, String localDirectory) throws GitCloneUriException  {
        String folderToReach = "";
        String cleanUrl;
        try {
            if (repositoryUrl.endsWith(".git")) {
                cleanUrl = repositoryUrl.replace(".git", "");
                repositoryUrl = cleanUrl;
            }
            this.pathToReach = alienTpmPath.resolve(localDirectory);
            Files.createDirectories(pathToReach);
            this.locations = branchMap;
            String folder = this.splitRepositoryName(repositoryUrl);
            if (isCompleteImport()) {
                cloneEntireRepository(repositoryUrl, pathToReach.resolve(folder + _ALL));
                folderToReach = pathToReach.resolve(folder + _ALL).toString();
                zipRepositoryToRoot(this.pathToReach.resolve(folder + _ALL));
            } else {
                cloneEntireRepository(repositoryUrl, pathToReach.resolve(folder + locations.size()));
                zipRepository(pathToReach.resolve(folder + locations.size()), this.getLocations());
                folderToReach = pathToReach.resolve(folder + locations.size()).toString();
            }

        } catch (IOException e) {
            log.error("Error while creating target directory ", e);
        }
        return folderToReach;
    }

    private boolean isCompleteImport() {
        if (locations.size() == 1) {
            for (Map.Entry<String, String> location : locations.entrySet()) {
                if (location.getKey() == null || location.getKey() == "") {
                    return true;
                }
            }
        }
        return false;
    }

    private void cloneRepository(String url, String branch, Path targetPath) throws IOException {
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
            FileUtil.delete(targetPath);
            log.error("Failed to clone git repository.", e);
        }
    }

    /**
     * Check and clone a repository from its url if the repository has already been checked-out
     * 
     * @param url Github url of the repository
     * @param branch Specified branch to clone
     * @param targetPath Path of the folder to checkout the repository
     * @throws GitCloneUriException 
     * @throws IOException 
     * @throws GitAPIException 
     */
    private void cloneEntireRepository(String url, Path targetPath) throws GitCloneUriException {
        Git result;
        log.info("Cloning from [" + url + "] to [" + targetPath.toString() + "]");
        try {
            result = Git.cloneRepository().setURI(url).setDirectory(targetPath.toFile()).call();
        } catch (Exception e) {
            if (e instanceof JGitInternalException) {
                log.info(e.getMessage());
                try {
                    FileUtil.delete(targetPath);
                    throw new GitCloneUriException(e.getMessage());
                } catch (IOException ioEx) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Zip a folder and its CSARs with any importLocations
     * 
     * If the repository is already and archive the folder is zipped directly
     * Else, only the sub-repos containing the Yaml file are zipped.
     * 
     * @param pathToFetch The path where the parent folder is stored
     * @param locations The sub-folders to zip
     */
    private void zipRepository(Path pathToFetch, Map<String, String> locations) {
        for (Entry<String, String> entry : locations.entrySet()) {
            File file = pathToFetch.resolve(entry.getKey()).toFile();
            try {
                File[] listFiles = file.listFiles();
                if (!isArchive(listFiles)) {
                    for (int i = 0; i < listFiles.length; i++) {
                        if (listFiles[i].isDirectory() && !listFiles[i].getName().endsWith(".git")) {
                            FileUtil.zip(file.toPath().resolve(listFiles[i].getName()), pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                            this.csarsToImport.add(pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                        }
                    }
                } else {
                    Path locatePath = file.toPath();
                    FileUtil.zip(locatePath, locatePath.resolve(entry.getKey() + _SUFFIXE));
                    this.csarsToImport.add(locatePath.resolve(entry.getKey() + _SUFFIXE));
                }
            } catch (IOException e) {
                log.error("Error while zipping target directory ", e);
            }
        }
    }

    /**
     * Zip a folder and its CSARs without any importLocations
     * 
     * If the repository is already and archive the folder is zipped directly
     * Else, only the sub-repos containing the Yaml file are zipped.
     * 
     * @param pathToFetch The path where the parent folder is stored
     */
    private void zipRepositoryToRoot(Path pathToFetch) {
        File file = pathToFetch.toFile();
        try {
            File[] listFiles = file.listFiles();
            if (!isArchive(listFiles)) {
                for (int i = 0; i < listFiles.length; i++) {
                    if (listFiles[i].isDirectory() && !listFiles[i].getName().endsWith(".git")) {
                        FileUtil.zip(file.toPath().resolve(listFiles[i].getName()), pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                        this.csarsToImport.add(pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                    }
                }
            } else {
                FileUtil.zip(pathToFetch, pathToFetch.resolve("../" + splitRepositoryName(file.getName() + _SUFFIXE)));
                this.csarsToImport.add(pathToFetch.resolve("../" + file.getName() + _SUFFIXE));
            }
        } catch (IOException e) {
            log.error("Error while zipping target directory ", e);
        }
    }

    /**
     * Check if the folder checked-out is an archive or not
     * 
     * @param listFiles Files of the directory
     * @return False if the repository isn't a directory
     * @return Yes if the repository is a directory
     */
    private boolean isArchive(File[] listFiles) {
        int cpt = 0;
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].isDirectory()) {
                cpt++;
            }
        }
        return cpt > 2 ? false : true;
    }

    /**
     * Retrieve the repository name of an url based on the last "/".
     * 
     * @param url The url to parse
     * @return The repository name
     */
    private String splitRepositoryName(String url) {
        String[] urlSplit = url.split("/");
        return urlSplit[urlSplit.length - 1];
    }
}