package alien4cloud.it.csars;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;
import alien4cloud.rest.csar.CreateCsarGitRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CsarGitCRUDStepDefinition {

    @When("^I add a GIT repository with url \"(.*?)\" usr \"(.*?)\" pwd \"(.*?)\" stored \"(.*?)\" and locations$")
    public void i_add_a_GIT_repository_with_url_usr_pwd_stored_and_locations(String url, String usr, String pwd, boolean stored,
            List<CsarGitCheckoutLocation> locations) throws Throwable {

        CreateCsarGitRequest request = new CreateCsarGitRequest();
        request.setRepositoryUrl(url);
        request.setUsername(usr);
        request.setPassword(pwd);
        request.setStoredLocally(stored);
        request.setImportLocations(locations);

        String response = Context.getRestClientInstance().postJSon("/rest/v1/csarsgit/", JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I list all the git repositories$")
    public void i_list_all_the_git_repositories() throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/csarsgit/");
        Context.getInstance().registerRestResponse(response);
    }

    @Then("^I should have (\\d+) git repository in the list$")
    public void i_should_have_git_repository_in_the_list(int repoGitCount) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        assertNull(response.getError());
        assertNotNull(response.getData());
        Assert.assertEquals(repoGitCount, response.getData().getTotalResults());
    }

    @Then("^I can find a GIT repository with url \"(.*?)\" usr \"(.*?)\" pwd \"(.*?)\" stored \"(.*?)\" and locations$")
    public void i_can_find_a_GIT_repository_with_url_usr_pwd_stored_and_locations(String url, String usr, String pwd, boolean stored,
            List<CsarGitCheckoutLocation> locations)
            throws Throwable {

        CsarGitRepository csarGitRepository = getCsarGitRepository(url);
        Assert.assertNotNull(csarGitRepository);

        // a comparator to sort CsarGitCheckoutLocation lists
        Comparator<CsarGitCheckoutLocation> comparator = new Comparator<CsarGitCheckoutLocation>() {
            @Override
            public int compare(CsarGitCheckoutLocation o1, CsarGitCheckoutLocation o2) {
                int bCompare = o1.getBranchId().compareTo(o2.getBranchId());
                if (bCompare != 0) {
                    return bCompare;
                } else {
                    return o1.getSubPath().compareTo(o2.getSubPath());
                }
            }
        };

        Assert.assertEquals(usr, csarGitRepository.getUsername());
        Assert.assertEquals(pwd, csarGitRepository.getPassword());
        Assert.assertEquals(stored, csarGitRepository.isStoredLocally());
        Assert.assertEquals(locations.size(), csarGitRepository.getImportLocations().size());
        Collections.sort(csarGitRepository.getImportLocations(), comparator);
        // locations is unmodifiable
        List<CsarGitCheckoutLocation> expectedlocations = new ArrayList<CsarGitCheckoutLocation>(locations);
        Collections.sort(expectedlocations, comparator);
        for (int i = 0; i < expectedlocations.size(); i++) {
            CsarGitCheckoutLocation expected = expectedlocations.get(i);
            CsarGitCheckoutLocation actual = csarGitRepository.getImportLocations().get(i);
            Assert.assertEquals(actual.getBranchId(), expected.getBranchId());
            Assert.assertEquals(actual.getSubPath(), expected.getSubPath());
        }
    }

    private CsarGitRepository getCsarGitRepository(String url) throws Throwable {
        RestResponse<GetMultipleDataResult> response = JsonUtil.read(Context.getInstance().getRestResponse(), GetMultipleDataResult.class);
        assertNull(response.getError());
        assertNotNull(response.getData());

        for (Object object : response.getData().getData()) {
            CsarGitRepository csarGitRepository = JsonUtil.readObject(JsonUtil.toString(object), CsarGitRepository.class);
            if (csarGitRepository.getRepositoryUrl().equals(url)) {
                return csarGitRepository;
            }
        }
        return null;
    }

    @Given("^I get the GIT repo with url \"(.*?)\"$")
    public void i_get_the_GIT_repo_with_url(String url) throws Throwable {
        i_list_all_the_git_repositories();
        CsarGitRepository csarGitRepository = getCsarGitRepository(url);
        Context.getInstance().setCsarGitRepositoryId(csarGitRepository.getId());
    }

    @When("^I update the GIT repository with url \"(.*?)\" usr \"(.*?)\" pwd \"(.*?)\" stored \"(.*?)\" and locations$")
    public void i_update_the_GIT_repository_with_url_usr_pwd_stored_and_locations(String url, String usr, String pwd, boolean stored,
            List<CsarGitCheckoutLocation> locations)
            throws Throwable {

        CreateCsarGitRequest request = new CreateCsarGitRequest();
        request.setRepositoryUrl(url);
        request.setUsername(usr);
        request.setPassword(pwd);
        request.setStoredLocally(stored);
        request.setImportLocations(locations);

        String restUrl = String.format("/rest/v1/csarsgit/%s", Context.getInstance().getCsarGitRepositoryId());
        String response = Context.getRestClientInstance().putJSon(restUrl, JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
    }

    @When("^I delete the GIT repository$")
    public void i_delete_the_GIT_repository() throws Throwable {
        i_delete_the_GIT_repository_with_id(Context.getInstance().getCsarGitRepositoryId());
    }

    @When("^I import the GIT repository$")
    public void i_import_the_GIT_repository() throws Throwable {
        String restUrl = String.format("/rest/v1/csarsgit/%s", Context.getInstance().getCsarGitRepositoryId());
        String response = Context.getRestClientInstance().postJSon(restUrl, "");
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I get the GIT repository with id \"(.*?)\"$")
    public void i_get_the_GIT_repository_with_id(String id) throws Throwable {
        String restUrl = String.format("/rest/v1/csarsgit/%s", id);
        String response = Context.getRestClientInstance().get(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I delete the GIT repository with id \"(.*?)\"$")
    public void i_delete_the_GIT_repository_with_id(String id) throws Throwable {
        String restUrl = String.format("/rest/v1/csarsgit/%s", id);
        String response = Context.getRestClientInstance().delete(restUrl);
        Context.getInstance().registerRestResponse(response);
    }

}
