package alien4cloud.it.users;

import static org.junit.Assert.*;

import org.apache.commons.lang3.ArrayUtils;

import alien4cloud.dao.model.FacetedSearchResult;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.security.AuthenticationStepDefinitions;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.User;
import alien4cloud.security.users.rest.UserSearchRequest;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class UsersDefinitionsSteps {

    private AuthenticationStepDefinitions authSteps = new AuthenticationStepDefinitions();

    @Given("^there is (\\d+) users in the system$")
    public void there_is_users_in_the_system(int count) throws Throwable {
        for (int i = 0; i < count; i++) {
            authSteps.I_create_a_new_user_with_name_and_password_in_the_system("user_" + i, "user");
        }
    }

    @When("^I search in users for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_search_in_users_for_from_with_result_size_of(String searchedText, int from, int size) throws Throwable {
        UserSearchRequest req = new UserSearchRequest(searchedText, null, from, size);
        String jSon = Context.getInstance().getJsonMapper().writeValueAsString(req);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/users/search", jSon));
    }

    @Then("^there should be (\\d+) users in the response$")
    public void there_should_be_users_in_the_response(int expectedSize) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), FacetedSearchResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getTypes().length);
        assertEquals(expectedSize, searchResp.getData().length);
    }

    @When("^I add a role \"([^\"]*)\" to user \"([^\"]*)\"$")
    public void I_add_a_role_to_user(String role, String username) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().put("/rest/v1/users/" + username + "/roles/" + role));
    }

    @Then("^The response should contain a user \"([^\"]*)\" having \"([^\"]*)\" role$")
    public void The_response_should_contain_a_user_having_role(String username, String expectedRole) throws Throwable {
        RestResponse<User> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), User.class);
        User user = restResponse.getData();
        assertNotNull(user);
        assertNotNull(user.getRoles());
        assertTrue(ArrayUtils.contains(user.getRoles(), expectedRole));
    }

    @Given("^there is a user \"([^\"]*)\" with the \"([^\"]*)\" role$")
    public void there_is_a_user_with_the_role(String username, String expectedRole) throws Throwable {
        authSteps.There_is_a_user_in_the_system(username);
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        User user = JsonUtil.read(response, User.class).getData();
        if (!ArrayUtils.contains(user.getRoles(), expectedRole)) {
            I_add_a_role_to_user(expectedRole, username);
        }
    }

    @When("^I remove a role \"([^\"]*)\" to user \"([^\"]*)\"$")
    public void I_remove_a_role_to_user(String role, String username) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/users/" + username + "/roles/" + role));
    }

    @Then("^The response should contain a user \"([^\"]*)\" not having \"([^\"]*)\" role$")
    public void The_response_should_contain_a_user_not_having_role(String username, String expectedRole) throws Throwable {
        RestResponse<User> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), User.class);
        User user = restResponse.getData();
        assertNotNull(user);
        assertTrue(!ArrayUtils.contains(user.getRoles(), expectedRole));
    }

    @When("^I delete the user \"([^\"]*)\"$")
    public void I_delete_the_user(String username) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/users/" + username));
    }

    @Then("^The response should contain a user \"([^\"]*)\"$")
    public void The_response_should_contain_a_user(String username) throws Throwable {
        RestResponse<FacetedSearchResult> restResponse = JsonUtil.read(Context.getInstance().getRestResponse(), FacetedSearchResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        boolean found = false;
        for (Object json : searchResp.getData()) {
           User user = JsonUtil.readObject(JsonUtil.toString(json), User.class);
           found = user.getUsername().equals(username);
           if (found) break;
        }
        assertTrue(found);
    }

}