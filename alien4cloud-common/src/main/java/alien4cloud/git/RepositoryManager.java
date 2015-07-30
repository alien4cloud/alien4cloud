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

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;

import alien4cloud.exception.GitCloneUriException;
import alien4cloud.exception.GitNotAuthorizedException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.utils.FileUtil;

import com.jcraft.jsch.Session;

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
    public static String _TOSCA_METADATA = "TOSCA-Metadata";

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
     * Clone or checkout a Git Repository and its sub-branch contained in a Map<Url,Branch>
     * 
     * @param alienTpmPath The folder to clone the repositories
     * @param login The username of the Git repository if private
     * @param password The password of the Git repository if private
     * @param repositoryUrl The Git url to reach the repository
     * @param branchMap A Map of the sub-repositories with its branchId (i.e : master,develop etc)
     * @param localDirectory Static path to be resolved with targetDirectory
     * @throws GitCloneUriException
     * @throws GitNotAuthorizedException
     * @throws GitAPIException
     */
    public String createFolderAndClone(Path alienTpmPath, String repositoryUrl, String username, String password, Map<String, String> branchMap,
            String localDirectory) throws GitCloneUriException, GitNotAuthorizedException {
        String folderToReach = "";
        try {
            this.pathToReach = alienTpmPath.resolve(localDirectory);
            Files.createDirectories(pathToReach);
            this.locations = branchMap;
            String folder = this.splitRepositoryName(repositoryUrl);
            if (isCompleteImport()) {
                cloneEntireRepository(repositoryUrl, username, password, pathToReach.resolve(folder + _ALL));
                folderToReach = pathToReach.resolve(folder + _ALL).toString();
                zipRepositoryToRoot(this.pathToReach.resolve(folder + _ALL));
            } else {
                cloneEntireRepository(repositoryUrl, username, password, pathToReach.resolve(folder + locations.size()));
                zipRepository(pathToReach.resolve(folder + locations.size()), this.getLocations());
                folderToReach = pathToReach.resolve(folder + locations.size()).toString();
            }

        } catch (IOException | NotFoundException e) {
            log.error("Error while creating target directory ", e);
        }
        return folderToReach;
    }

    /**
     * Detect if the requested Git repository contains one or more folders to import
     * 
     * @return True, false otherwise.
     */
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
     * @param url Git url of the repository
     * @param branch Specified branch to clone
     * @param targetPath Path of the folder to checkout the repository
     * @throws GitCloneUriException
     * @throws GitNotAuthorizedException
     * @throws IOException
     * @throws GitAPIException
     */
    private void cloneEntireRepository(String url, String username, final String password, Path targetPath) throws GitCloneUriException,
            GitNotAuthorizedException {
        Git result = null;
        log.info("Cloning from [" + url + "] to [" + targetPath.toString() + "]");

        if (username != "" || password != "") {
            try {
                UsernamePasswordCredentialsProvider cp = new UsernamePasswordCredentialsProvider(username, password);
                result = Git.cloneRepository().setURI(url).setDirectory(targetPath.toFile()).setCredentialsProvider(cp).call();
                try {
                    log.info("Cloned: " + result.getRepository().getDirectory());
                } finally {
                    result.close();
                }
            } catch (Exception e) {
                this.handleGitException(e, targetPath);
            }
        } else {
            try {
                result = Git.cloneRepository().setURI(url).setDirectory(targetPath.toFile()).call();
                try {
                    log.info("Cloned: " + result.getRepository().getDirectory());
                } finally {
                    result.close();
                }
            } catch (Exception e) {
                this.handleGitException(e, targetPath);
            }
        }
    }

    /**
     * Zip a folder and its CSARs with importLocations to fetch
     * 
     * If the repository is already and archive the folder is zipped directly
     * Else, only the sub-repos containing the Yaml file are zipped.
     * 
     * @param pathToFetch The path where the parent folder is located
     * @param locations The sub-folders to zip
     */
    private void zipRepository(Path pathToFetch, Map<String, String> locations) {
        for (Entry<String, String> entry : locations.entrySet()) {
            File file = pathToFetch.resolve(entry.getKey()).toFile();
            if (file.exists()) {
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
                        FileUtil.zip(locatePath, pathToFetch.resolve(entry.getKey() + _SUFFIXE));
                        this.csarsToImport.add(pathToFetch.resolve(entry.getKey() + _SUFFIXE));
                    }
                } catch (IOException e) {
                    log.error("Error while zipping target directory ", e);
                }
            } else {
                throw new NotFoundException(file.getName());
            }
        }
    }

    /**
     * Zip a folder and its CSARs without any importLocations
     * 
     * If the repository is already and archive the folder is zipped directly
     * Else, only the sub-repos containing the Yaml file are zipped.
     * 
     * @param pathToFetch The path where the parent folder is located
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
     * @return Yes if the repository is a directory or if the repository is based on an older TOSCA recommendation
     */
    private boolean isArchive(File[] listFiles) {
        int cpt = 0;
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].getName().equals(_TOSCA_METADATA) || listFiles[i].getName().endsWith(".yaml") || listFiles[i].getName().endsWith(".yml")
                    || listFiles[i].getName().equals("csar")) {
                return true;
            }
            if (listFiles[i].isDirectory() && !(listFiles[i].getName().endsWith(".git"))) {
                cpt++;
            }
        }
        return cpt >= 2 ? false : true;
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

    /**
     * Handle exception's throw regarding Git callback
     * 
     * @param gitException Git exception when cloning
     * @throws GitCloneUriException Exception when the repository doesn't exists
     * @throws GitNotAuthorizedException Exception when the user doesn't the sufficient privileges
     */
    private void handleGitException(Exception gitException, Path targetPath) throws GitCloneUriException, GitNotAuthorizedException {
        if (gitException instanceof JGitInternalException) {
            try {
                FileUtil.delete(targetPath);
                throw new GitCloneUriException(gitException.getMessage());
            } catch (IOException ioEx) {
            }
        }
        if (gitException instanceof InvalidRemoteException) {
            try {
                FileUtil.delete(targetPath);
                throw new GitCloneUriException(gitException.getMessage());
            } catch (IOException ioEx) {
            }
        }
        if (gitException instanceof TransportException) {
            try {
                FileUtil.delete(targetPath);
                throw new GitNotAuthorizedException(gitException.getMessage());
            } catch (IOException ioEx) {
            }
        }

    }
}