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
import org.springframework.beans.factory.annotation.Value;

import alien4cloud.exception.AlreadyExistException;
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
     */
    public void createFolderAndClone(Path alienTpmPath, String repositoryUrl, Map<String, String> branchMap, String localDirectory) {
        try {
            this.pathToReach = alienTpmPath.resolve(localDirectory);
            Files.createDirectories(pathToReach);
            this.locations = branchMap;
            String folder = this.splitRepositoryName(repositoryUrl);
            for (Map.Entry<String, String> location : locations.entrySet()) {
                if (location.getKey() == null || location.getKey() == "") {
                    cloneRepositoryWithCheck(repositoryUrl, pathToReach.resolve(folder));
                    zipRepository(this.pathToReach.resolve(folder));
                } else {
                    cloneRepositoryWithCheck(repositoryUrl, pathToReach.resolve(folder));
                    zipRepository(this.pathToReach.resolve(folder).resolve(location.getKey()), this.getLocations());
                }
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


    /**
     * Check and clone a repository from its url if the repository has already been checked-out
     * 
     * @param url Github url of the repository
     * @param branch Specified branch to clone
     * @param targetPath Path of the folder to checkout the repository
     */
    private void cloneRepositoryWithCheck(String url, Path targetPath) {
        Git result;
        if (!Files.exists(targetPath)) {
            log.info("Cloning from [" + url + "] to [" + targetPath.toString() + "]");
            try {
                result = Git.cloneRepository().setURI(url).setDirectory(targetPath.toFile()).call();
                try {
                    log.info("Cloned: " + result.getRepository().getDirectory());
                } finally {
                    result.close();
                }
            } catch (GitAPIException e) {
                log.error("Failed to clone git repository.", e);
            }
        } else {
            log.error("Csar " + targetPath + " have been already cloned from Github");
            throw new AlreadyExistException("Csar have been already cloned from Github");
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
        File file = pathToFetch.toFile();
        for (Entry<String, String> entry : locations.entrySet()) {
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
                    FileUtil.zip(pathToFetch, pathToFetch.resolve(entry.getKey() + _SUFFIXE));
                    this.csarsToImport.add(pathToFetch.resolve(entry.getKey() + _SUFFIXE));
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
    private void zipRepository(Path pathToFetch) {
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
                FileUtil.zip(pathToFetch, pathToFetch.resolve(splitRepositoryName(file.getName() + _SUFFIXE)));
                this.csarsToImport.add(pathToFetch.resolve(file.getName() + _SUFFIXE));
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
     * @param url The url to parse
     * @return The repository name
     */
    private String splitRepositoryName(String url) {
        String[] urlSplit = url.split("/");
        return url.split("/")[urlSplit.length-1];
    }
}