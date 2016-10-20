package alien4cloud.csar.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.catalog.index.ICsarService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.google.common.collect.Lists;

import alien4cloud.common.AlienConstants;
import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.GitException;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.CSARSource;
import alien4cloud.model.git.CsarDependenciesBean;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CsarGitService {
    @Inject
    private CsarGitRepositoryService csarGitRepositoryService;
    @Inject
    private CsarFinderService csarFinderService;
    @Inject
    private ArchiveUploadService uploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
    @Resource
    private ICsarService csarService;
    // TODO store archives that are not 'temp' in another location.
    private Path tempDirPath;
    private Path tempZipDirPath;

    @Required
    @Value("${directories.alien}/${directories.upload_temp}")
    public void setTempDirPath(String tempDirPath) throws IOException {
        this.tempDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath + "/git");
        this.tempZipDirPath = FileUtil.createDirectoryIfNotExists(tempDirPath + "/gitzips");
    }

    /**
     * Delete an CsarGitRepository based on its id.
     *
     * @param id The id of the CsarGitRepository to delete.
     */
    public void delete(String id) {
        CsarGitRepository csarGit = csarGitRepositoryService.getOrFail(id);
        if (csarGit.isStoredLocally()) {
            Path repositoryPath = tempDirPath.resolve(csarGit.getId());
            if (Files.isDirectory(repositoryPath)) {
                FileSystemUtils.deleteRecursively(repositoryPath.toFile());
            }
        }
        alienDAO.delete(CsarGitRepository.class, csarGit.getId());
    }

    /**
     * Import Cloud Service ARchives from a git repository.
     *
     * @param id The id to access the git repository.
     * @return The result of the import (that may contains errors etc.)
     */
    public List<ParsingResult<Csar>> importFromGitRepository(String id) {
        CsarGitRepository csarGitRepository = csarGitRepositoryService.getOrFail(id);

        List<ParsingResult<Csar>> results = Lists.newArrayList();
        try {
            // Iterate over locations (branches, folders etc.) and process the import
            for (CsarGitCheckoutLocation csarGitCheckoutLocation : csarGitRepository.getImportLocations()) {
                List<ParsingResult<Csar>> result = doImport(csarGitRepository, csarGitCheckoutLocation);
                if (result != null) {
                    results.addAll(result);
                }
            }
        } finally {
            // cleanup
            Path archiveZipRoot = tempZipDirPath.resolve(csarGitRepository.getId());
            Path archiveGitRoot = tempDirPath.resolve(csarGitRepository.getId());
            try {
                FileUtil.delete(archiveZipRoot);
                if (!csarGitRepository.isStoredLocally()) {
                    FileUtil.delete(archiveGitRoot);
                }
            } catch (IOException e) {
                log.error("Failed to cleanup files after git import.", e);
            }
        }

        return results;
    }

    private List<ParsingResult<Csar>> doImport(CsarGitRepository csarGitRepository, CsarGitCheckoutLocation csarGitCheckoutLocation) {
        Git git = null;
        try {
            // checkout the repository branch
            git = RepositoryManager.cloneOrCheckout(tempDirPath, csarGitRepository.getRepositoryUrl(), csarGitRepository.getUsername(),
                    csarGitRepository.getPassword(), csarGitCheckoutLocation.getBranchId(), csarGitRepository.getId());
            // if the repository is persistent we also have to pull to get the latest version
            if (csarGitRepository.isStoredLocally()) {
                // try to pull
                RepositoryManager.pull(git, csarGitRepository.getUsername(), csarGitRepository.getPassword());
            }
            String hash = RepositoryManager.getLastHash(git);

            // now that the repository is checked out and up to date process with the import
            List<ParsingResult<Csar>> results = processImport(csarGitRepository, csarGitCheckoutLocation, hash);

            if (!Objects.equals(csarGitCheckoutLocation.getLastImportedHash(), hash)) {
                csarGitCheckoutLocation.setLastImportedHash(hash);
                alienDAO.save(csarGitRepository); // update the hash for this location.
            }
            // TODO best would be to provide with a better result to show that we didn't retried import
            return results;
        } finally {
            RepositoryManager.close(git);
        }
    }

    private List<ParsingResult<Csar>> processImport(CsarGitRepository csarGitRepository, CsarGitCheckoutLocation csarGitCheckoutLocation, String gitHash) {
        // find all the archives under the given hierarchy and zip them to create archives
        Path archiveZipRoot = tempZipDirPath.resolve(csarGitRepository.getId());
        Path archiveGitRoot = tempDirPath.resolve(csarGitRepository.getId());
        Set<Path> archivePaths = csarFinderService.prepare(archiveGitRoot, archiveZipRoot, csarGitCheckoutLocation.getSubPath());

        // TODO code review has to be completed to further cleanup below processing.
        List<ParsingResult<Csar>> parsingResult = Lists.newArrayList();
        try {
            Map<CSARDependency, CsarDependenciesBean> csarDependenciesBeans = uploadService.preParsing(archivePaths, parsingResult);
            List<CsarDependenciesBean> sorted = sort(csarDependenciesBeans);
            for (CsarDependenciesBean csarBean : sorted) {
                if (csarGitCheckoutLocation.getLastImportedHash() != null && csarGitCheckoutLocation.getLastImportedHash().equals(gitHash)) {
                    if (csarService.get(csarBean.getSelf().getName(), csarBean.getSelf().getVersion()) != null) {
                        // no commit since last import and the archive still exist in the repo, so do not import
                        // TODO notify the user that the archive has already been imported
                        continue;
                    }
                }
                // FIXME Add possibility to choose an workspace
                ParsingResult<Csar> result = uploadService.upload(csarBean.getPath(), CSARSource.GIT, AlienConstants.GLOBAL_WORKSPACE_ID);
                parsingResult.add(result);
            }
            return parsingResult;
        } catch (ParsingException e) {
            // TODO Actually add a parsing result with error.
            throw new GitException("Failed to import archive from git as it cannot be parsed", e);
        } catch (AlreadyExistException e) {
            return parsingResult;
        } catch (CSARUsedInActiveDeployment e) {
            // TODO Actually add a parsing result with error.
            return parsingResult;
        }
    }

    private List<CsarDependenciesBean> sort(Map<CSARDependency, CsarDependenciesBean> elements) {
        List<CsarDependenciesBean> sortedCsars = Lists.newArrayList();

        List<CsarDependenciesBean> independents = Lists.newArrayList();
        for (Map.Entry<CSARDependency, CsarDependenciesBean> entry : elements.entrySet()) {
            CsarDependenciesBean csar = entry.getValue();
            if (csar.getDependencies() == null) {
                // the element has no dependencies
                independents.add(csar);
            } else {
                // complete the list of dependent elements
                List<CSARDependency> toClears = Lists.newArrayList();
                for (CSARDependency dependent : csar.getDependencies()) {
                    CsarDependenciesBean providedDependency = elements.get(dependent);
                    if (providedDependency == null) {
                        // remove the dependency as it may be in the alien repo
                        toClears.add(dependent);
                    } else {
                        providedDependency.getDependents().add(entry.getValue());
                    }
                }
                for (CSARDependency toClear : toClears) {
                    csar.getDependencies().remove(toClear);
                }
                if (csar.getDependencies().isEmpty()) {
                    independents.add(csar);
                }
            }
        }

        while (independents.size() > 0) {
            CsarDependenciesBean independent = independents.remove(0);
            elements.remove(independent.getSelf()); // remove from the elements
            sortedCsars.add(independent); // element has no more dependencies
            for (CsarDependenciesBean dependent : independent.getDependents()) {
                dependent.getDependencies().remove(independent.getSelf());
                if (dependent.getDependencies().isEmpty()) {
                    independents.add(dependent);
                }
            }
        }

        if (elements.size() > 0) {
            // TODO there is looping dependencies throw exception or ignore ?
        }

        return sortedCsars;
    }
}