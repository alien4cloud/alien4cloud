package alien4cloud.it.csars;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.analysis.util.CharArrayMap.EntrySet;
import org.junit.Assert;
import org.junit.Test;

import alien4cloud.it.Context;
import alien4cloud.rest.component.SearchRequest;
import alien4cloud.rest.csar.CreateCsarGithubRequest;
import alien4cloud.rest.csar.CsarInfoDTO;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.CsarGitCheckoutLocation;
import alien4cloud.security.model.CsarGitRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class CsarGitCRUDStepDefinition {
    private CsarGitRepository CSAR_GIT_REPOSITORY;
    private CreateCsarGithubRequest request;

    @Given("^I have a csargit with the url \"([^\"]*)\" with username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Create_a_new_csargit(String url, String username, String password) throws JsonProcessingException, IOException {
        request = new CreateCsarGithubRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRepositoryUrl(url);
    }

    @And("^I add locations to the csar$")
    public void I_add_locations_to_the_csar(List<CsarGitCheckoutLocation> locations) throws Throwable {
        request.setImportLocations(locations);
    }

    @When("I create a csargit")
    public void I_Create_a_new_Csar_From_Git() throws Throwable {
        String response = Context.getRestClientInstance().postJSon("/rest/csarsgit/", JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
        Object id = JsonUtil.read(response).getData();
        if (!(id instanceof List<?>)) {
            String csargitId = (String) id;
            Context.getInstance().saveCsarGitId(csargitId, request.getRepositoryUrl());
        }
    }

    @And("I delete a csargit with id")
    public void I_Delete_a_csargit_with_id() throws Throwable {
        String id = "";
        for (Entry<String, String> entry : Context.getInstance().getCsarGitId().entrySet()) {
            if (entry.getValue().equals("https://github.com/alien4cloud/tosca-normative-types")) {
                id = entry.getKey();
            }
        }
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/csarsgit/" + id));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNull(restResponse.getError());
    }

    @And("I delete a csargit with url \"([^\"]*)\"")
    public void I_Delete_a_csargit_with_url(String url) throws Throwable {
        int sizeBefore = Context.getInstance().getCsarGitId().size();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/delete/:url", url));
        int sizeAfter = Remove_CsarGitList(url);
        Assert.assertNotEquals(sizeAfter, sizeBefore);
    }

    public boolean I_have_CSARGIT_created_with_id(String csarId) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/csarsgit/" + csarId));
        CsarGitRepository csargit = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarGitRepository.class).getData();
        if (csargit == null) {
            return false;
        }
        return true;
    }

    public int Remove_CsarGitList(Object obj) {
        for (Entry<String, String> entry : Context.getInstance().getCsarGitId().entrySet()) {
            if (entry.getValue().equals(obj)) {
                Context.getInstance().getCsarGitId().remove(obj);
            }
        }
        return Context.getInstance().getCsarGitId().size();
    }

    @When("I have no csargit created with id")
    public void I_Have_no_csargit_created_with_id() throws Throwable {
        Assert.assertFalse(I_have_CSARGIT_created_with_id(CSAR_GIT_REPOSITORY.getId()));
    }

    @And("I have no csargit created with url \"([^\"]*)\"")
    public void I_Have__csargit_created_with_url(String url) throws Throwable {
        Assert.assertTrue(I_have_CSARGIT_created_with_url(url));
    }

    @And("I have a csargit created with url \"([^\"]*)\"")
    public void I_Have_a_csargit_created_with_url(String url) throws Throwable {
        Assert.assertTrue(!I_have_CSARGIT_created_with_url(url));
    }

    public boolean I_have_CSARGIT_created_with_url(String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/get/", JsonUtil.toString(url)));
        CsarGitRepository csargit = JsonUtil.read(Context.getInstance().takeRestResponse(), CsarGitRepository.class).getData();
        if (csargit == null) {
            return false;
        }
        Assert.assertNotNull(csargit);
        return true;
    }

    @When("I trigger the import of the csars")
    public void I_Trigger_the_import_of_the_csars() throws Throwable {
        for (Entry<String, String> obj : Context.getInstance().getCsarGitId().entrySet()) {
            Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/import/:id", obj.getKey()));
            RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
            Assert.assertNotNull(restResponse);
            Assert.assertNotNull(restResponse.getError());
        }
    }

    @And("I trigger the import of a csar with url \"([^\"]*)\"")
    public void I_Trigger_the_import_of_a_csar_with_url(String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/import/:id", url));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
        Assert.assertNotNull(restResponse.getError());
    }
}
