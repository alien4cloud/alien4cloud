package alien4cloud.it.csars;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import alien4cloud.it.Context;
import alien4cloud.rest.csar.CreateCsarGithubRequest;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.CsarGitCheckoutLocation;
import alien4cloud.security.model.CsarGitRepository;

public class CsarGithubImport {
    private CsarGitRepository CSAR_GITHUB_REPOSITORY;

    @Test
    public void I_Have_all_Csar_data(String id, String url, String username, String password, List<CsarGitCheckoutLocation> locations) {
        CSAR_GITHUB_REPOSITORY = new CsarGitRepository();
        CSAR_GITHUB_REPOSITORY.setPassword(password);
        CSAR_GITHUB_REPOSITORY.setImportLocations(locations);
        CSAR_GITHUB_REPOSITORY.setUsername(username);
        CSAR_GITHUB_REPOSITORY.setRepositoryUrl(url);
    }

    @Test
    public void I_Create_a_new_Csar_From_Git() throws Throwable {
        CreateCsarGithubRequest request = new CreateCsarGithubRequest(CSAR_GITHUB_REPOSITORY.getRepositoryUrl(),
                CSAR_GITHUB_REPOSITORY.getUsername(), CSAR_GITHUB_REPOSITORY.getPassword(), CSAR_GITHUB_REPOSITORY.getImportLocations());
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/", JsonUtil.toString(request)));
    }

    @Test
    public void I_Delete_a_Csar_with_id(String id) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/csarsgit/" + id));
        String response = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Assert.assertNull(response);
    }

    @Test
    public void I_Update_a_Csar_with_id_and_url(String id, String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put("/rest/csarsgit/" + id));
    }

    @Test
    public boolean I_have_a_csar_from_git_created_with_id(String id) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/csarsgit/" + id));
        CsarGitRepository csargit = JsonUtil.read(Context.getInstance().getRestResponse(), CsarGitRepository.class).getData();
        if (csargit == null) {
            return false;
        }
        Assert.assertNotNull(csargit);
        Assert.assertEquals(csargit.getId(), id);
        return true;
    }
}
