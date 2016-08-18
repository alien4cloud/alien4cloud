package alien4cloud.it.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;

import alien4cloud.it.Context;
import alien4cloud.it.Entry;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.users.rest.CreateUserRequest;
import alien4cloud.security.model.User;
import cucumber.api.PendingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class AuthenticationStepDefinitions {

    @When("^I retrieve the ALIEN's roles list$")
    public void I_retrieve_the_ALIEN_s_roles_list() throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/auth/roles"));
    }

    @Given("^I am logged out$")
    public void I_am_logged_out() throws Throwable {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Context.getRestClientInstance().postUrlEncoded("/logout", nvps);
        Context.getRestClientInstance().clearCookies();
    }

    @When("^I authenticate with username \"([^\"]*)\" and password \"([^\"]*)\"$")
    public void I_authenticate_with_username_and_password(String username, String password) throws Throwable {
        I_am_logged_out();
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("username", username));
        nvps.add(new BasicNameValuePair("password", password));
        nvps.add(new BasicNameValuePair("submit", "Login"));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postUrlEncoded("/login", nvps));
    }

    @When("^I create a new user with username \"([^\"]*)\" and password \"([^\"]*)\" in the system$")
    public void I_create_a_new_user_with_name_and_password_in_the_system(String username, String password) throws Throwable {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(username + "@alien4cloud.org");
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/users/", JsonUtil.toString(request)));
    }

    @When("^I authenticate with \"([^\"]*)\" role$")
    public void I_authenticate_with_role(String role) throws Throwable {
        I_am_authenticated_with_role(role);
    }

    @Given("^I am authenticated with \"([^\"]*)\" role$")
    public void I_am_authenticated_with_role(String role) throws Throwable {
        I_am_logged_out();
        switch (role) {
        case "ADMIN":
            I_authenticate_with_username_and_password("admin", "admin");
            break;
        case "COMPONENTS_BROWSER":
            I_authenticate_with_username_and_password("componentBrowser", "componentBrowser");
            break;
        case "COMPONENTS_MANAGER":
            I_authenticate_with_username_and_password("componentManager", "componentManager");
            break;
        case "APPLICATIONS_MANAGER":
            I_authenticate_with_username_and_password("applicationManager", "applicationManager");
            break;
        case "APP_MANAGER":
            I_authenticate_with_username_and_password("appManager", "appManager");
            break;
        case "ARCHITECT":
            I_authenticate_with_username_and_password("architect", "architect");
            break;
        case "USER":
            I_authenticate_with_username_and_password("user", "user");
            break;
        default:
            throw new PendingException();
        }
    }

    @Given("^There is a \"([^\"]*)\" user in the system$")
    public void There_is_a_user_in_the_system(String username) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        RestResponse<User> userResponse = JsonUtil.read(response, User.class);
        if (userResponse.getData() == null) {
            I_create_a_new_user_with_name_and_password_in_the_system(username, username);
        }
    }

    @Given("^There are these users in the system$")
    public void There_are_these_users_in_the_system(List<String> usernames) throws Throwable {
        for (String username : usernames) {
            There_is_a_user_in_the_system(username);
        }
    }

    @Given("^There is no \"([^\"]*)\" user in the system$")
    public void There_is_no_user_in_the_system(String username) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        RestResponse<User> userResponse = JsonUtil.read(response, User.class);
        if (userResponse.getData() != null) {
            I_remove_user(username);
        }
    }

    @When("^I get the \"([^\"]*)\" user$")
    public void I_get_the_user(String username) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/users/" + username));
    }

    @When("^I remove user \"([^\"]*)\"$")
    public void I_remove_user(String username) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/users/" + username));
    }

    @When("^I find users with usernames$")
    public void I_find_users_with_usersnames(List<String> usernames) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().postJSon("/rest/v1/users/getUsers", Context.getInstance().getJsonMapper().writeValueAsString(usernames)));
    }

    @When("^I find users with an empty usernames list$")
    public void I_find_users_with_an_empty_usersnames_list() throws Throwable {
        I_find_users_with_usersnames(Lists.<String> newArrayList());
    }

    @SuppressWarnings("unchecked")
    @Then("^the find RestResponse should have the users with usersnames$")
    public void the_find_RestResponse_should_have_the_users_with_usersnames(List<String> expectedUsernames) throws Throwable {
        List<Object> users = JsonUtil.read(Context.getInstance().getRestResponse(), List.class).getData();
        assertNotNull(users);
        List<String> usernames = Lists.newArrayList();
        for (Object obj : users) {
            User user = Context.getInstance().getJsonMapper().readValue(Context.getInstance().getJsonMapper().writeValueAsString(obj), User.class);
            usernames.add(user.getUsername());
        }
        assertEquals(expectedUsernames.size(), usernames.size());
        assertTrue(usernames.containsAll(expectedUsernames));
    }

    @Given("^I am authenticated with user named \"([^\"]*)\"$")
    public void I_am_authenticated_with_user_named(String user) throws Throwable {
        I_authenticate_with_username_and_password(user, user);
    }

    @When("^I update \"([^\"]*)\" user's fields:$")
    public void I_update_user_s_fields(String username, List<Entry> fields) throws Throwable {
        Map<String, String> fieldsMap = Maps.newHashMap();
        for (Entry field : fields) {
            fieldsMap.put(field.getName(), field.getValue());
        }
        String resp = Context.getRestClientInstance().putJSon("/rest/v1/users/" + username, JsonUtil.toString(fieldsMap));
        System.out.println(resp);
        Context.getInstance().registerRestResponse(resp);

    }

    @Then("^There should be a user \"([^\"]*)\" with firstname \"([^\"]*)\" in the system$")
    public void There_should_be_a_user_with_firstname_in_the_system(String username, String expectedFirstName) throws Throwable {
        User user = JsonUtil.read(Context.getRestClientInstance().get("/rest/v1/users/" + username), User.class).getData();
        assertNotNull(user);
        assertEquals(expectedFirstName, user.getFirstName());
    }
}
