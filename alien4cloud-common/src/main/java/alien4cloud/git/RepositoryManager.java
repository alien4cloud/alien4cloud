package alien4cloud.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;

import alien4cloud.utils.FileUtil;

/**
 * Utility to manage git repositories.
 */
@Slf4j
public class RepositoryManager {
    @Getter
    private ArrayList<Path> csarsToImport = new ArrayList<Path>();
    @Getter
    private Path pathToReach;
    @Value("${directories.alien}/${directories.upload_temp}")
    private String alienTempUpload;
    public static String _SUFFIXE = "_ZIPPED.zip";
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
     * Clone and zip
     * 
     * @param alienTpmPath The folder to clone the repositories
     * @param repositoryUrl The GitHub url to reach the repository
     * @param branch Name of branch (i.e : master,develop etc)
     * @param localDirectory Static path to be resolved with targetDirectory
     */
    public void createFolderAndCloneAndZip(Path alienTpmPath, String repositoryUrl, List<String> branchs, String localDirectory) {
        try {
            this.pathToReach = alienTpmPath.resolve(localDirectory);
            Files.createDirectories(pathToReach);
            for (String branch : branchs) {
                cloneRepositoryWithCheck(repositoryUrl, branch, pathToReach.resolve(branch));
            }
            zipRepository(this.pathToReach, branchs);
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
    private void cloneRepositoryWithCheck(String url, String branch, Path targetPath) {
        Git result;
        if (!Files.exists(targetPath)) {
            log.info("Cloning from [" + url + "] branch [" + branch + "] to [" + targetPath.toString() + "]");
            try {
                result = Git.cloneRepository().setURI(url).setBranch(branch).setDirectory(targetPath.toFile()).call();
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
         //   throw new AlreadyExistException("Csar have been already cloned from Github");
        }
    }

    /**
     * Zip a folder and its CSARs
     * 
     * If the repository is already and archive the folder is zippe directly
     * Else, only the sub-repos containing the Yaml file are zipped.
     * 
     * @param pathToFetch The path where the parent folder is stored
     * @param locations The sub-folders to zip
     */
    private void zipRepository(Path pathToFetch, List<String> branchs) {
        File file = pathToFetch.toFile();
        for (String branch : branchs) {
            try {
                file = pathToFetch.resolve(branch).toFile();
                File[] listFiles = file.listFiles();
                if (!isArchive(listFiles)) {
                    for (int i = 0; i < listFiles.length; i++) {
                        if (listFiles[i].isDirectory() && !listFiles[i].getName().endsWith(".git")) {
                            FileUtil.zip(file.toPath().resolve(listFiles[i].getName()), pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                            this.csarsToImport.add(pathToFetch.resolve(listFiles[i].getName() + _SUFFIXE));
                        }
                    }
                } else {
                    FileUtil.zip(pathToFetch.resolve(branch), pathToFetch.resolve(branch + _SUFFIXE));
                    this.csarsToImport.add(pathToFetch.resolve(branch + _SUFFIXE));
                }
            } catch (IOException e) {
                log.error("Error while zipping target directory ", e);
            }
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
}