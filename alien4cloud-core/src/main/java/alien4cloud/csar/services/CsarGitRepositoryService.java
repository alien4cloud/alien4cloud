package alien4cloud.csar.services;

import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.exception.InvalidArgumentException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.utils.UrlUtil;
import org.springframework.stereotype.Service;

/**
 * Manages operations on a CsarGitRepository
 */
@Service
public class CsarGitRepositoryService {
    private static final String URL_FIELD = "repositoryUrl";
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    /**
     * Create a CsarGitRepository in the system to store its informations
     *
     * @param repositoryUrl The unique Git url of the CsarGitRepository
     * @param username The username of the user
     * @param password The password of the user
     * @param importLocations Locations where Csar's files are store
     * @param isStoredLocally The state of the the CsarGitRepository
     * @return The auto-generated id of the CsarGitRepository object
     */
    public String create(String repositoryUrl, String username, String password, List<CsarGitCheckoutLocation> importLocations, boolean isStoredLocally) {
        validatesRepositoryUrl(repositoryUrl);
        if (importLocations.isEmpty()) {
            throw new InvalidArgumentException("Import locations cannot be empty.");
        }
        // create it
        CsarGitRepository csarGit = new CsarGitRepository();
        csarGit.setId(UUID.randomUUID().toString());
        csarGit.setRepositoryUrl(repositoryUrl);
        csarGit.setUsername(username);
        csarGit.setPassword(password);
        csarGit.setImportLocations(importLocations);
        csarGit.setStoredLocally(isStoredLocally);
        alienDAO.save(csarGit);
        return csarGit.getId();
    }

    private void validatesRepositoryUrl(String repositoryUrl) {
        // check if the repository url has a valid format
        if (!UrlUtil.isValid(repositoryUrl)) {
            throw new InvalidArgumentException("Repository url <" + repositoryUrl + "> is not a valid url.");
        }
        // and that the repository doesn't already exists
        if (alienDAO.customFind(CsarGitRepository.class, QueryBuilders.termQuery(URL_FIELD, repositoryUrl)) != null) {
            throw new AlreadyExistException("A repository with url <" + repositoryUrl + "> already exists in alien 4 cloud.");
        }
    }

    /**
     * Get the csar git repository matching the given id or throw a NotFoundException
     *
     * @param id If of the csar git repository that we want to get.
     * @return An instance of the csar git repository.
     */
    public CsarGitRepository getOrFailById(String id) {
        CsarGitRepository csarGitRepository = alienDAO.findById(CsarGitRepository.class, id);
        if (csarGitRepository == null) {
            throw new NotFoundException("CSAR Git repository [" + id + "] doesn't exists.");
        }
        return csarGitRepository;
    }

    /**
     * Get a csar git repository matching the given repository url or throw a NotFoundException if none can be found for the given url.
     *
     * @param url The URL of the repository
     * @return An instance of the csar git repository
     */
    public CsarGitRepository getCsargitByUrl(String url) {
        CsarGitRepository csarGitRepository = alienDAO.customFind(CsarGitRepository.class, QueryBuilders.termQuery(URL_FIELD, url));
        if (csarGitRepository == null) {
            throw new NotFoundException("CSAR Git repository [" + url + "] doesn't exists.");
        }
        return csarGitRepository;
    }

    /**
     * Get the csar git repository matching the given id or throw a NotFoundException.
     *
     * @param idOrUrl The id or url to access the git repository.
     * @return An instance of the csar git repository matching the given id or url.
     */
    public CsarGitRepository getOrFail(String idOrUrl) {
        if (UrlUtil.isValid(idOrUrl)) {
            return getCsargitByUrl(idOrUrl);
        }
        return getOrFailById(idOrUrl);
    }

    /**
     * Get multiple csar git repositories.
     *
     * @param query The query to apply to filter csar git repositories.
     * @param from The start index of the query.
     * @param size The maximum number of elements to return.
     * @return A {@link GetMultipleDataResult} that contains CsarGitRepository objects.
     */
    public GetMultipleDataResult<CsarGitRepository> search(String query, int from, int size) {
        return alienDAO.search(CsarGitRepository.class, query, null, from, size);
    }

    /**
     * Update informations for a given CsarGitRepository.
     *
     * @param idOrUrl The id or url of the CsarGitRepository to update.
     * @param repositoryUrl The new url of the CsarGitRepository
     * @param username The username associated to the CsarGitRepository
     * @param password The password associated to the CsarGitRepository
     */
    public void update(String idOrUrl, String repositoryUrl, String username, String password) {
        CsarGitRepository repositoryToUpdate = getOrFail(idOrUrl);
        if (repositoryUrl != null) {
            validatesRepositoryUrl(repositoryUrl);
            repositoryToUpdate.setRepositoryUrl(repositoryUrl);
        }
        if (username != null) {
            repositoryToUpdate.setUsername(username);
        }
        if (password != null) {
            repositoryToUpdate.setPassword(password);
        }
        alienDAO.save(repositoryToUpdate);
    }
}
