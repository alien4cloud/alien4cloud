package alien4cloud.csar.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import javax.annotation.Resource;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jgit.api.Git;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.GitException;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.CSARDependency;
import alien4cloud.model.components.Csar;
import alien4cloud.security.model.CsarDependenciesBean;
import alien4cloud.security.model.CsarGitCheckoutLocation;
import alien4cloud.security.model.CsarGitRepository;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;

import com.google.common.collect.Lists;

@Component
@Slf4j
public class CsarGitService {
    @Inject
    private CsarGitRepositoryService csarGitRepositoryService;
    @Inject
    private CsarFinderService csarFinderService;
    @Inject
    private ArchiveUploadService uploadService;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;
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
     * Delete an CsarGitRepository based on its id or url.
     *
     * @param idOrUrl The id or url of the CsarGitRepository to update.
     */
    public void delete(String idOrUrl) {
        CsarGitRepository csarGit = csarGitRepositoryService.getOrFail(idOrUrl);
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
     * @param idOrUrl The id or url to access the git repository.
     * @return The result of the import (that may contains errors etc.)
     */
    public List<ParsingResult<Csar>> importFromGitRepository(String idOrUrl) {
        CsarGitRepository csarGitRepository = csarGitRepositoryService.getOrFail(idOrUrl);

        List<ParsingResult<Csar>> results = Lists.newArrayList();
        try {
            // Iterate over locations to be imported within the CsarGitRepository
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
        // checkout the repository branch
        Git git = RepositoryManager.cloneOrCheckout(tempDirPath, csarGitRepository.getRepositoryUrl(), csarGitRepository.getUsername(),
                csarGitRepository.getPassword(), csarGitCheckoutLocation.getBranchId(), csarGitRepository.getId());
        // if the repository is persistent we also have to pull to get the latest version
        if (csarGitRepository.isStoredLocally()) {
            // try to pull
            RepositoryManager.pull(git, csarGitRepository.getUsername(), csarGitRepository.getPassword());
        }
        String hash = RepositoryManager.getLastHash(git);
        if (csarGitCheckoutLocation.getLastImportedHash() != null && csarGitCheckoutLocation.getLastImportedHash().equals(hash)) {
            return null; // no commit since last import.
        }
        // now that the repository is checked out and up to date process with the import
        List<ParsingResult<Csar>> result = processImport(csarGitRepository, csarGitCheckoutLocation);
        csarGitCheckoutLocation.setLastImportedHash(hash);
        alienDAO.save(csarGitRepository); // update the hash for this location.
        return result;
    }

    private List<ParsingResult<Csar>> processImport(CsarGitRepository csarGitRepository, CsarGitCheckoutLocation csarGitCheckoutLocation) {
        // find all the archives under the given hierarchy and zip them to create archives
        Path archiveZipRoot = tempZipDirPath.resolve(csarGitRepository.getId());
        Path archiveGitRoot = tempDirPath.resolve(csarGitRepository.getId());
        Set<Path> archivePaths = csarFinderService.prepare(archiveGitRoot, archiveZipRoot);

        // TODO code review has to be completed to further cleanup below processing.
        List<ParsingResult<Csar>> parsingResult = new ArrayList<ParsingResult<Csar>>();
        List<CsarDependenciesBean> csarDependenciesBeanList = null;
        try {
            csarDependenciesBeanList = uploadService.preParsing(archivePaths);
            for (CsarDependenciesBean dep : csarDependenciesBeanList) {
                if (dep.getDependencies() == null || dep.getDependencies().isEmpty()) {
                    parsingResult.add(uploadService.upload(dep.getPath()));
                    return parsingResult;
                }
            }
            this.updateDependenciesList(csarDependenciesBeanList);
            parsingResult = this.handleImportLogic(csarDependenciesBeanList, parsingResult);
            return parsingResult;
        } catch (ParsingException e) {
            // TODO Actually add a parsing result with error.
            throw new GitException("Failed to import archive from git as it cannot be parsed", e);
        } catch (CSARVersionAlreadyExistsException e) {
            return parsingResult;
        }
    }

    /**
     * Update the CsarDependenciesBean list based on the CSARDependency found (i.e : deleted or imported
     *
     * @param csarDependenciesBeanList List containing the CsarDependenciesBean
     */
    private void updateDependenciesList(List<CsarDependenciesBean> csarDependenciesBeanList) {
        for (CsarDependenciesBean csarContainer : csarDependenciesBeanList) {
            Iterator<?> it = csarContainer.getDependencies().iterator();
            while (it.hasNext()) {
                CSARDependency dependencie = (CSARDependency) it.next();
                if (lookupForDependencie(dependencie, csarDependenciesBeanList) == null) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Process to a lookup in the CsarDependenciesBean list to check if a CSARDependency is in the list
     *
     * @param dependencie
     * @param csarDependenciesBeanList
     * @return the csarBean matching the dependency
     */
    private CsarDependenciesBean lookupForDependencie(CSARDependency dependencie, List<CsarDependenciesBean> csarDependenciesBeanList) {
        for (CsarDependenciesBean csarBean : csarDependenciesBeanList) {
            if ((dependencie.getName() + ":" + dependencie.getVersion()).equals(csarBean.getName() + ":" + csarBean.getVersion())) {
                return csarBean;
            }
        }
        return null;
    }

    /**
     * Method to import a repository based on its inter-dependencies
     *
     * @param csarDependenciesBeanList List of the CsarGitRepository to import
     * @param parsingResult Result of the pre-process parsing
     * @return The result of the final import
     * @throws CSARVersionAlreadyExistsException
     * @throws ParsingException
     */
    private List<ParsingResult<Csar>> handleImportLogic(List<CsarDependenciesBean> csarDependenciesBeanList, List<ParsingResult<Csar>> parsingResult)
            throws CSARVersionAlreadyExistsException, ParsingException {
        ParsingResult<Csar> result = null;
        for (CsarDependenciesBean csarBean : csarDependenciesBeanList) {
            if (csarBean.getDependencies().isEmpty()) {
                result = uploadService.upload(csarBean.getPath());
                removeExistingDependencies(csarBean, csarDependenciesBeanList);
                parsingResult.add(result);
            } else {
                Iterator<?> it = csarBean.getDependencies().iterator();
                while (it.hasNext()) {
                    CSARDependency dep = (CSARDependency) it.next();
                    CsarDependenciesBean bean = lookupForDependencie(dep, csarDependenciesBeanList);
                    this.analyseCsarBean(result, bean, parsingResult, csarDependenciesBeanList);
                }
            }
        }
        return parsingResult;
    }

    /**
     * Analyse a CsarDependenciesBean to check if it is ready to import
     *
     * @param result Result of the pre-parsing process
     * @param bean Bean containing the CsarGitRepository informations
     * @param parsingResult Global result of the pre-parsing result
     * @param csarDependenciesBeanList
     * @throws CSARVersionAlreadyExistsException
     * @throws ParsingException
     */
    private void analyseCsarBean(ParsingResult<Csar> result, CsarDependenciesBean bean, List<ParsingResult<Csar>> parsingResult,
            List<CsarDependenciesBean> csarDependenciesBeanList) throws CSARVersionAlreadyExistsException, ParsingException {
        if (bean != null) {
            result = uploadService.upload(bean.getPath());
            parsingResult.add(result);
            removeExistingDependencies(bean, csarDependenciesBeanList);
        }
    }

    /**
     * Remove all the occurences of the dependencie when uploaded
     *
     * @param bean Bean representing the Csar to import with detailed data
     * @param csarDependenciesBeanList The list containing the other CsarDependenciesBean
     */
    private void removeExistingDependencies(CsarDependenciesBean bean, List<CsarDependenciesBean> csarDependenciesBeanList) {
        Set<?> tmpSet;
        for (CsarDependenciesBean csarBean : csarDependenciesBeanList) {
            // To avoid concurrentmodificationexception we clone the set before fetching
            tmpSet = new HashSet(csarBean.getDependencies());
            Iterator<?> it = tmpSet.iterator();
            while (it.hasNext()) {
                CSARDependency dep = (CSARDependency) it.next();
                if ((dep.getName() + ":" + dep.getVersion()).equals(bean.getName() + ":" + bean.getVersion())) {
                    it.remove();
                }
            }
            csarBean.setDependencies(tmpSet);
        }
    }
}