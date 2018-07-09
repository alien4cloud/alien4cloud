package alien4cloud.csar.services;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.CSARSource;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingErrorLevel;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.tosca.parser.impl.ErrorCode;
import alien4cloud.utils.AlienConstants;
import alien4cloud.utils.FileUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.ArchiveUploadService;
import org.alien4cloud.tosca.catalog.exception.UploadExceptionUtil;
import org.alien4cloud.tosca.catalog.index.CsarService;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.CsarDependenciesBean;
import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.annotation.Resource;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private CsarService csarService;
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
            log.debug("Checkout repository: url: [ {} ] branch [ {} ] to [ {} ]", csarGitRepository.getRepositoryUrl(), csarGitCheckoutLocation.getBranchId(),
                    tempDirPath);
            git = RepositoryManager.cloneOrCheckout(tempDirPath, csarGitRepository.getRepositoryUrl(), csarGitRepository.getUsername(),
                    csarGitRepository.getPassword(), csarGitCheckoutLocation.getBranchId(), csarGitRepository.getId());
            // if the repository is persistent we also have to pull to get the latest version
            if (csarGitRepository.isStoredLocally() && RepositoryManager.isBranch(git, csarGitCheckoutLocation.getBranchId())) {
                // try to pull
                log.debug("Pull local repository");
                RepositoryManager.pull(git, csarGitRepository.getUsername(), csarGitRepository.getPassword());
            }
            String hash = RepositoryManager.getLastHash(git);

            log.debug("Importing archives from git repository, pulled hash is {}", hash);
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
        if (csarGitCheckoutLocation.getSubPath() != null && !csarGitCheckoutLocation.getSubPath().isEmpty()) {
            archiveGitRoot = archiveGitRoot.resolve(csarGitCheckoutLocation.getSubPath());
        }
        Set<Path> archivePaths = csarFinderService.prepare(archiveGitRoot, archiveZipRoot);

        List<ParsingResult<Csar>> parsingResults = Lists.newArrayList();

        Map<CSARDependency, CsarDependenciesBean> csarDependenciesBeans = uploadService.preParsing(archivePaths, parsingResults);
        List<CsarDependenciesBean> sorted = sort(csarDependenciesBeans);
        for (CsarDependenciesBean csarBean : sorted) {
            String archiveRepoPath = archiveZipRoot.relativize(csarBean.getPath().getParent()).toString();
            if (csarGitCheckoutLocation.getLastImportedHash() != null && csarGitCheckoutLocation.getLastImportedHash().equals(gitHash)
                    && csarService.get(csarBean.getSelf().getName(), csarBean.getSelf().getVersion()) != null) {
                // no commit since last import and the archive still exist in the repo, so do not import
                addAlreadyImportParsingResult(archiveRepoPath, parsingResults);
                continue;
            }
            try {
                // FIXME Add possibility to choose an workspace
                ParsingResult<Csar> result = uploadService.upload(csarBean.getPath(), CSARSource.GIT, AlienConstants.GLOBAL_WORKSPACE_ID);
                result.getContext().setFileName(archiveRepoPath + "/" + result.getContext().getFileName());
                parsingResults.add(result);
            } catch (ParsingException e) {
                ParsingResult<Csar> failedResult = new ParsingResult<>();
                failedResult.setContext(new ParsingContext(archiveRepoPath));
                failedResult.getContext().setParsingErrors(e.getParsingErrors());
                parsingResults.add(failedResult);
                log.debug("Failed to import archive from git as it cannot be parsed", e);
            } catch (AlreadyExistException | ToscaTypeAlreadyDefinedInOtherCSAR | CSARUsedInActiveDeployment e) {
                ParsingResult<Csar> failedResult = new ParsingResult<>();
                failedResult.setContext(new ParsingContext(archiveRepoPath));
                failedResult.getContext().setParsingErrors(Lists.newArrayList(UploadExceptionUtil.parsingErrorFromException(e)));
                parsingResults.add(failedResult);
            }
        }
        return parsingResults;

    }

    /**
     * Just add a parsing info to the list stating that the archive is already imported and
     *
     * @param archivePath The path of the archive in the repo.
     * @param parsingResults The list of parsing results.
     */
    private void addAlreadyImportParsingResult(String archivePath, List<ParsingResult<Csar>> parsingResults) {
        ParsingResult<Csar> result = new ParsingResult<>();
        result.setContext(new ParsingContext(archivePath));
        result.getContext().setParsingErrors(Lists.newArrayList(new ParsingError(ParsingErrorLevel.INFO, ErrorCode.CSAR_ALREADY_INDEXED,
                "No new commit since last import and archive already indexed.", null, null, null, null

        )));
        parsingResults.add(result);
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
                for (CSARDependency dependency : csar.getDependencies()) {
                    CsarDependenciesBean providedDependency = elements.get(dependency);
                    if (providedDependency == null) {
                        // remove the dependency as it may be in the alien repo
                        toClears.add(dependency);
                    } else {
                        providedDependency.getDependents().add(csar);
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

        while (!independents.isEmpty()) {
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