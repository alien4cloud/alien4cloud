package alien4cloud.it.csars;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;

import alien4cloud.it.Context;
import alien4cloud.rest.csar.AddCsarGitLocation;
import alien4cloud.rest.csar.CreateCsarGitRequest;
import alien4cloud.rest.csar.UpdateCsarGitRequest;
import alien4cloud.rest.csar.UpdateCsarGitWithUrlRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.model.git.CsarGitCheckoutLocation;
import alien4cloud.model.git.CsarGitRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;

public class CsarGitCRUDStepDefinition {
    private CsarGitRepository CSAR_GIT_REPOSITORY;
    private CreateCsarGitRequest request;
    private UpdateCsarGitRequest request_update;
    private AddCsarGitLocation request_location;

    @Given("^I get an unexisting CsarGitRepository with id \"([^\"]*)\"$")
    public void I_get_a_csargit_by_id(String id) throws IOException {
        String response = Context.getRestClientInstance().get("/rest/csarsgit/" + id);
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I get an unexisting CsarGitRepository with null id$")
    public void I_get_a_csargit_with_null_id() throws IOException {
        String response = Context.getRestClientInstance().get("/rest/csarsgit/" + null);
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I get an existing CsarGitRepository with an url \"([^\"]*)\"$")
    public void I_get_a_existing_csargit_by_id(String url) throws IOException {
        String csarId = getIdByUrl(url);
        String response = Context.getRestClientInstance().get("/rest/csarsgit/" + csarId);
        Context.getInstance().registerRestResponse(response);
    }

    public String getIdByUrl(String url) {
        String id = "";
        for (Entry<String, String> entry : Context.getInstance().getCsarGitInfos().entrySet()) {
            if (entry.getValue().equals(url)) {
                id = entry.getKey();
                break;
            }
        }
        return id;
    }

    @Given("^I update a csar with url \"([^\"]*)\" and username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Update_a_csar_with_url(String url, String username, String password) throws Throwable {
        String id = getIdByUrl(url);
        I_Update_a_csargit_by_id(id, url, username, password);

    }

    @Given("^I update a csar with url \"([^\"]*)\" and new url \"([^\"]*)\" and username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Update_a_csar_with_url(String url, String newUrl, String username, String password) throws Throwable {
        String id = getIdByUrl(url);
        I_Update_a_csargit_by_id(id, newUrl, username, password);

    }

    @Given("^I add location to an unexisting CsarGitRepository with id \"([^\"]*)\"$")
    public void I_add_location_to_an_unexisting_csargit(String id) throws IOException {
        CsarGitCheckoutLocation location = new CsarGitCheckoutLocation();
        location.setBranchId("master");
        location.setSubPath("apache");
        List<CsarGitCheckoutLocation> importLocations = new ArrayList<CsarGitCheckoutLocation>();
        importLocations.add(location);
        request_location = new AddCsarGitLocation();
        request_location.setImportLocations(importLocations);
        String response = Context.getRestClientInstance().putJSon("/rest/csarsgit/:id/importLocations", JsonUtil.toString(request_location));
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I get an unexisting CsarGitRepository with url \"([^\"]*)\"$")
    public void I_get_a_csargit_by_url(String url) throws IOException {
        String response = Context.getRestClientInstance().postJSon("/rest/csarsgit/get", url);
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I get an unexisting CsarGitRepository with null url$")
    public void I_get_a_csargit_with_null_url() throws IOException {
        String url = null;
        String response = Context.getRestClientInstance().postJSon("/rest/csarsgit/get", url);
        Context.getInstance().registerRestResponse(response);
    }

    @Given("^I have a csargit with empty data and url \"([^\"]*)\" with username \"([^\"]*)\" and stored \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Create_a_new_csargit_with_empty_data(String url, String username, String password, String storedLocally)
            throws JsonProcessingException, IOException {
        request = new CreateCsarGitRequest();
        request.setUsername(null);
        request.setPassword(null);
        request.setRepositoryUrl(url);
        request.setStoredLocally(false);
    }

    @Given("^I have a csargit with the url \"([^\"]*)\" with username \"([^\"]*)\" and stored \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Create_a_new_csargit(String url, String username, String password, String storedLocally) throws JsonProcessingException, IOException {
        request = new CreateCsarGitRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRepositoryUrl(url);
        request.setStoredLocally(false);
    }

    @Given("^I have a csargit with the url \"([^\"]*)\" with username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Create_a_new_csargit_with_saving(String url, String username, String password) throws JsonProcessingException, IOException {
        request = new CreateCsarGitRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRepositoryUrl(url);
        request.setStoredLocally(true);
    }

    @And("^I add locations to the csar$")
    public void I_add_locations_to_the_csar(List<CsarGitCheckoutLocation> locations) throws Throwable {
        request.setImportLocations(locations);
    }

    @And("^I add empty locations to the csar$")
    public void I_add_empty_locations_to_the_csar(List<CsarGitCheckoutLocation> locations) throws Throwable {
        request.setImportLocations(null);
    }

    @When("I create a csargit")
    public void I_Create_a_new_Csar_From_Git() throws Throwable {
        String response = Context.getRestClientInstance().postJSon("/rest/csarsgit/", JsonUtil.toString(request));
        Context.getInstance().registerRestResponse(response);
        Object id = JsonUtil.read(response).getData();
        if (!(id instanceof List<?>)) {
            String csargitId = (String) id;
            Context.getInstance().saveCsarGitInfos(csargitId, request.getRepositoryUrl());
        }
    }

    @And("I delete a csargit with id")
    public void I_Delete_a_csargit_with_id() throws Throwable {
        String id = "";
        Iterator entries = Context.getInstance().getCsarGitInfos().entrySet().iterator();
        while (entries.hasNext()) {
            Entry entry = (Entry) entries.next();
            if (entry.getValue().equals("https://github.com/alien4cloud/tosca-normative-types")) {
                id = (String) entry.getKey();
                entries.remove();
            }
        }
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/csarsgit/" + id));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNull(restResponse.getError());
    }

    @And("I delete a csargit with url \"([^\"]*)\"")
    public void I_Delete_a_csargit_with_url(String url) throws Throwable {
        int sizeBefore = Context.getInstance().getCsarGitInfos().size();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/delete/:url", url));
        int sizeAfter = Remove_CsarGitList(url);
        Assert.assertNotEquals(sizeAfter, sizeBefore);
    }

    @And("I delete a csargit with a wrong url \"([^\"]*)\"")
    public void I_Delete_a_csargit_with_a_wrong_url(String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/delete/:url", url));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse.getError());
    }

    @Given("I delete a csargit with an url \"([^\"]*)\"")
    public void I_Delete_a_csargit_with_empty_url(String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/delete/:url", url));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse.getError());
    }

    @And("I delete a csargit with wrong url \"([^\"]*)\"")
    public void I_Delete_a_csargit_with_wrong_url(String url) throws Throwable {
        int sizeBefore = Context.getInstance().getCsarGitInfos().size();
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/delete/:url", url));
        int sizeAfter = Remove_CsarGitList(url);
        Assert.assertEquals(sizeAfter, sizeBefore);
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
        Iterator entries = Context.getInstance().getCsarGitInfos().entrySet().iterator();
        while (entries.hasNext()) {
            Entry entry = (Entry) entries.next();
            if (entry.getValue().equals(obj)) {
                entries.remove();
            }
        }
        return Context.getInstance().getCsarGitInfos().size();
    }

    @When("I have no csargit created with id")
    public void I_Have_no_csargit_created_with_id() throws Throwable {
        Assert.assertFalse(I_have_CSARGIT_created_with_id(CSAR_GIT_REPOSITORY.getId()));
    }

    @And("I have no csargit created with url \"([^\"]*)\"")
    public void I_Have__csargit_created_with_url(String url) throws Throwable {
        Assert.assertTrue(!I_have_CSARGIT_created_with_url(url));
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

    @When("I trigger the import of the csars with url \"([^\"]*)\" and \"([^\"]*)\"")
    public void I_Trigger_the_import_of_the_csars(String url, String url2) throws Throwable {
        for (Entry<String, String> obj : Context.getInstance().getCsarGitInfos().entrySet()) {
            if (obj.getValue().equals(url) || obj.getValue().equals(url2)) {
                Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/import/:id", obj.getKey()));
                RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
                Assert.assertNotNull(restResponse);
                Assert.assertNull(restResponse.getError());
            }
        }
    }

    @And("I trigger the import of a csar with url \"([^\"]*)\"")
    public void I_Trigger_the_import_of_a_csar_with_url(String url) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/import/:id", url));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
    }

    @When("I update a csargit with url \"([^\"]*)\" and username \"([^\"]*)\" and password \"([^\"]*)\" and target url \"([^\"]*)\"$")
    public void I_Update_a_csargit_with_url(String url, String username, String password, String repositoryUrlToUpdate) throws Throwable {
        UpdateCsarGitWithUrlRequest request = new UpdateCsarGitWithUrlRequest();
        request.setRepositoryUrl(url);
        request.setUsername(username);
        request.setPassword(password);
        request.setPreviousRepositoryUrl(repositoryUrlToUpdate);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/csarsgit/update/:url", JsonUtil.toString(request)));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
    }

    @Given("I update a csargit with id \"([^\"]*)\" and url \"([^\"]*)\" and username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_Update_a_csargit_by_id(String id, String url, String username, String password) throws Throwable {
        this.request_update = new UpdateCsarGitRequest();
        request_update.setRepositoryUrl(url);
        request_update.setUsername(username);
        request_update.setPassword(password);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().putJSon("/rest/csarsgit/" + id, JsonUtil.toString(request_update)));
        RestResponse<?> restResponse = JsonUtil.read(Context.getInstance().getRestResponse());
        Assert.assertNotNull(restResponse);
    }

}
