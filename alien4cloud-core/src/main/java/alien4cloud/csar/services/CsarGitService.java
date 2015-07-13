package alien4cloud.csar.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import alien4cloud.component.repository.exception.CSARVersionAlreadyExistsException;
import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.GitCloneUriException;
import alien4cloud.exception.NotFoundException;
import alien4cloud.git.RepositoryManager;
import alien4cloud.model.components.Csar;
import alien4cloud.security.model.CsarGitCheckoutLocation;
import alien4cloud.security.model.CsarGitRepository;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;
import alien4cloud.utils.ReflectionUtil;

@Component
public class CsarGitService {
    @Resource
    ArchiveUploadService uploadService;

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDAO;

    @Value("${directories.alien}/${directories.upload_temp}")
    private String alienTempUpload;

    public final static String _LOCALDIRECTORY = "csarFromGit";

    /**
     * Create a CsarGitRepository in the system to store its informations
     * 
     * @param id The unique if of the CsarGitRepository
     * @param repositoryUrl The unique Git url of the CsarGitRepository
     * @param username The username of the user
     * @param password The password of the user
     * @param importLocations Locations where Csar's files are store
     * @return The auto-generated id of the CsarGitRepository object
     */
    public String createGitCsar(String repositoryUrl, String username, String password, List<CsarGitCheckoutLocation> importLocations) {
        CsarGitRepository csarGit = new CsarGitRepository();
        csarGit.setRepositoryUrl(repositoryUrl);
        csarGit.setUsername(username);
        csarGit.setPassword(password);
        csarGit.setImportLocations(importLocations);
        alienDAO.save(csarGit);
        return csarGit.getId();
    };

    /**
     * Method to trigger the checkout of a CsarGitRepository from Git
     * 
     * @param param The unique id of the CsarGitRepository to trigger
     * @return An response if the statement was successful or not
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     * @throws IOException
     * @throws GitCloneUriException
     */
    public ParsingResult<Csar>[] specifyCsarFromGit(String param) throws CSARVersionAlreadyExistsException, ParsingException, IOException, GitCloneUriException {
        CsarGitRepository csarGit = new CsarGitRepository();
        Map<String, String> locationsMap = new HashMap<String, String>();
        String data = param.replaceAll("\"", "");
        if (!paramIsUrl(data)) {
            csarGit = alienDAO.findById(CsarGitRepository.class, data);
        } else {
            csarGit = getCsargitByUrl(data);
        }
        RepositoryManager repoManager = new RepositoryManager();
        Path alienTpmPath = Paths.get(alienTempUpload);
        if (csarGit == null) {
            throw new NotFoundException("CsarGit " + "[" + data + "] doesn't exist");
        }
        for (CsarGitCheckoutLocation location : csarGit.getImportLocations()) {
            locationsMap.put(location.getSubPath(), location.getBranchId());
        }
        String var = repoManager.createFolderAndClone(alienTpmPath, csarGit.getRepositoryUrl(), locationsMap, _LOCALDIRECTORY);
        return triggerImportFromTmpFolder(var, repoManager.getCsarsToImport());
    }

    /**
     * Handle the import of CsarGitRepository (importLocations) in Alien
     * 
     * @param folderPath The path to fetch where Csars have been checked out.
     * @return An response if the statement was successful or not
     * @throws ParsingException
     * @throws CSARVersionAlreadyExistsException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public ParsingResult<Csar>[] triggerImportFromTmpFolder(String pathToReach, ArrayList<Path> csarsToImport) throws CSARVersionAlreadyExistsException,
            ParsingException, IOException {
        ArrayList<ParsingResult<Csar>> parsingResult = new ArrayList<ParsingResult<Csar>>();
        ParsingResult<Csar> result = null;
        for (Path path : csarsToImport) {
            result = uploadService.upload(path);
            parsingResult.add(result);
        }
        try {
            Path path = Paths.get(pathToReach);
            Path pathZipped = Paths.get(pathToReach.concat("_ZIPPED"));
            if (Files.exists(pathZipped)) {
                FileUtil.delete(pathZipped);
            }
            FileUtil.delete(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsingResult.toArray(new ParsingResult[parsingResult.size()]);
    }

    /**
     * Method to update a CsarGitRepository based on its unique id
     * 
     * @param id The unique id of the CsarGitRepository
     * @param request UpdateCsarGitRequest which contains all the required data to update the object
     */
    public void update(String id, String repositoryUrl, String username, String password) {
        CsarGitRepository csarGit = checkIfCsarExist(id);
        CsarGitRepository csarFrom = new CsarGitRepository();
        csarFrom.setId(id);
        csarFrom.setRepositoryUrl(repositoryUrl);
        csarFrom.setUsername(username);
        csarFrom.setPassword(password);
        if (csarGit != null) {
            ReflectionUtil.mergeObject(csarFrom, csarGit);
            alienDAO.save(csarGit);
        }
    };

    /**
     * Add a location into an existing CsarGitRepository object
     * 
     * @param id The unique id of the CsarGitRepository
     * @param locations Locations to add
     */
    public void addImportLocation(String id, List<CsarGitCheckoutLocation> locations) {
        if (locations.isEmpty() || locations == null) {
            throw new NotFoundException("Checkout location is empty");
        }
        CsarGitRepository csarGit = checkIfCsarExist(id);
        if (csarGit != null) {
            csarGit.getImportLocations().addAll(locations);
            alienDAO.save(csarGit);
        }
    };

    /**
     * Remove a location of an existing CsarGitRepository object
     * 
     * @param id The unique id of the CsarGitRepository
     * @param branchId The unique branch id
     */
    public void removeImportLocationById(String id, String branchId) {
        CsarGitRepository csarGit = checkIfCsarExist(id);
        if (csarGit == null) {
            throw new NotFoundException("CsarGit [" + id + "] cannot be found");
        }
        List<CsarGitCheckoutLocation> locations = csarGit.getImportLocations();
        if (CollectionUtils.isEmpty(locations)) {
            throw new NotFoundException("CsarGit import locations[" + id + " " + branchId + "]+ cannot be found");
        }
        Iterator<CsarGitCheckoutLocation> it = locations.iterator();
        while (it.hasNext()) {
            if (it.next().getBranchId().equals(branchId)) {
                it.remove();
                break;
            }
        }
        csarGit.setImportLocations(locations);
        alienDAO.save(csarGit);
    };

    /**
     * Remove a location of an existing CsarGitRepository object
     * 
     * @param url The unique id of the CsarGitRepository
     * @param branchId The unique branch id
     */
    public void removeImportLocationByUrl(String url, String branchId) {

        CsarGitRepository csarGit = getCsargitByUrl(url);
        if (csarGit == null) {
            throw new NotFoundException("CsarGit [" + url + "] cannot be found");
        }
        List<CsarGitCheckoutLocation> locations = csarGit.getImportLocations();
        if (CollectionUtils.isEmpty(locations)) {
            throw new NotFoundException("CsarGit import locations[" + url + "] is empty");
        }
        Iterator<CsarGitCheckoutLocation> it = locations.iterator();
        while (it.hasNext()) {
            if (it.next().getBranchId().equals(branchId)) {
                it.remove();
                break;
            }
        }
        csarGit.setImportLocations(locations);
        alienDAO.save(csarGit);
    }

    /**
     * Query elastic search to retrieve a CsarGit by its url
     * 
     * @param url The unique URL of the repository
     * @return The Repository with the URL
     */
    public CsarGitRepository getCsargitByUrl(String url) {
        CsarGitRepository csarGit = alienDAO.customFind(CsarGitRepository.class, QueryBuilders.termQuery("repositoryUrl", url));
        return csarGit;
    }

    /**
     * Delete an CsarGitRepository based on its URL
     * 
     * @param url The unique URL of the repository
     * @return The URL deleted
     */
    public String deleteCsargitByUrl(String url) {
        CsarGitRepository csarGit = getCsargitByUrl(url);
        if (csarGit != null) {
            alienDAO.delete(CsarGitRepository.class, QueryBuilders.termQuery("repositoryUrl", url));
            return url;
        }
        throw new NotFoundException("CsarGit [" + url + "] does not exist");
    }

    /**
     * Retrieve the CsarGitRepository based on its id
     * 
     * @param id The unique id of the CsarGitRepository
     * @return A CsarGitRepository object or a a new {@link NotFoundException}.
     */
    public CsarGitRepository checkIfCsarExist(String id) {
        CsarGitRepository csarGit = alienDAO.findById(CsarGitRepository.class, id);
        if (csarGit == null) {
            throw new NotFoundException("CsarGit [" + id + "] does not exist");
        }
        return csarGit;
    }

    /**
     * Check if the parameter is either an URL or an ID
     * 
     * @param data Data used to query the CsarGit
     * @return True is the pattern match the parameter with the regex, else false
     */
    public boolean paramIsUrl(String data) {
        String regex = "^(https?|git|http)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
        Pattern urlPattern = Pattern.compile(regex);
        Matcher m = urlPattern.matcher(data);
        return m.matches();
    }
}