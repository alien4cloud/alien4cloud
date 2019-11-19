package alien4cloud.it.groups;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.it.Context;
import alien4cloud.it.Entry;
import alien4cloud.it.common.CommonStepDefinitions;
import alien4cloud.it.users.UsersDefinitionsSteps;
import alien4cloud.rest.model.FilteredSearchRequest;
import alien4cloud.rest.model.RestResponse;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.security.model.User;
import alien4cloud.security.groups.rest.CreateGroupRequest;
import alien4cloud.security.model.Group;

import com.google.common.collect.Sets;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

@Slf4j
public class GroupsStepDefinitions {
    private CommonStepDefinitions commonSteps = new CommonStepDefinitions();
    private UsersDefinitionsSteps userSteps = new UsersDefinitionsSteps();

    @When("^I create a new group with name \"([^\"]*)\" in the system$")
    public void I_create_a_new_group_with_name_in_the_system(String name) throws Throwable {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(name);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/groups/", JsonUtil.toString(request)));
    }

    @Given("^There is a \"([^\"]*)\" group in the system$")
    public void There_is_a_group_in_the_system(String groupName) throws Throwable {
        I_have_created_the_group(groupName);
    }

    @Given("^I have created the group \"([^\"]*)\"$")
    public void I_have_created_the_group(String name) throws Throwable {
        I_create_a_new_group_with_name_in_the_system(name);
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
        String groupId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        Context.getInstance().registerGroupId(name, groupId);
    }

    @When("^I get the \"([^\"]*)\" group$")
    public void I_get_the_group(String name) throws Throwable {
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().get("/rest/v1/groups/" + Context.getInstance().getGroupId(name)));
    }

    @Then("^The RestResponse should contain a group with name \"([^\"]*)\"$")
    public void The_RestResponse_should_contain_a_group_with_name(String name) throws Throwable {
        RestResponse<Group> response = JsonUtil.read(Context.getInstance().getRestResponse(), Group.class);
        Group group = response.getData();
        assertNotNull(group);
        assertEquals(name, group.getName());
    }

    @When("^I delete the \"([^\"]*)\" group$")
    public void I_delete_the_group(String name) throws Throwable {
        String groupId = Context.getInstance().getGroupId(name);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().delete("/rest/v1/groups/" + groupId));
    }

    @Then("^There should not be a group \"([^\"]*)\" in the system$")
    public void There_should_not_be_a_group_in_the_system(String name) throws Throwable {
        I_get_the_group(name);
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
        commonSteps.I_should_receive_a_RestResponse_with_no_data();
    }

    @Given("^There are groups in the system$")
    public void There_are_groups_in_the_system(List<String> names) throws Throwable {
        for (String name : names) {
            There_is_a_group_in_the_system(name);
        }
    }

    @When("^I search in groups for \"([^\"]*)\" from (\\d+) with result size of (\\d+)$")
    public void I_search_in_groups_for_from_with_result_size_of(String query, int from, int size) throws Throwable {
        FilteredSearchRequest request = new FilteredSearchRequest(query, from, size, null);
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/groups/search", JsonUtil.toString(request)));
    }

    @Then("^there should be (\\d+) groups in the response$")
    public void there_should_be_groups_in_the_response(int expectedSize) throws Throwable {
        RestResponse<GetMultipleDataResult> restResponse = JsonUtil.read(Context.getInstance().takeRestResponse(), GetMultipleDataResult.class);
        GetMultipleDataResult searchResp = restResponse.getData();
        assertNotNull(searchResp);
        assertNotNull(searchResp.getTypes());
        assertNotNull(searchResp.getData());
        assertEquals(expectedSize, searchResp.getTypes().length);
        assertEquals(expectedSize, searchResp.getData().length);
    }

    @When("^I add the role \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void I_add_the_role_to_the_group(String role, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().put("/rest/v1/groups/" + Context.getInstance().getGroupId(groupName) + "/roles/" + role));
    }

    @Then("^the group \"([^\"]*)\" should have the following roles$")
    public void the_group_should_have_the_following_roles(String groupName, List<String> expectedRoles) throws Throwable {
        I_get_the_group(groupName);
        RestResponse<Group> response = JsonUtil.read(Context.getInstance().getRestResponse(), Group.class);
        Group group = response.getData();
        assertNotNull(group);
        assertNotNull(group.getRoles());
        assertEquals(Sets.newHashSet(expectedRoles), group.getRoles());
    }

    @Given("^I have added to the group \"([^\"]*)\" roles$")
    public void I_have_added_to_the_group_roles(String groupName, List<String> roles) throws Throwable {
        for (String role : roles) {
            I_add_the_role_to_the_group(role, groupName);
            commonSteps.I_should_receive_a_RestResponse_with_no_error();
        }
    }

    @When("^I remove the role \"([^\"]*)\" from the group \"([^\"]*)\"$")
    public void I_remove_the_role_from_the_group(String role, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete("/rest/v1/groups/" + Context.getInstance().getGroupId(groupName) + "/roles/" + role));
    }

    @When("^I add the user \"([^\"]*)\" to the group \"([^\"]*)\"$")
    public void I_add_the_user_to_the_group(String username, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().put("/rest/v1/groups/" + Context.getInstance().getGroupId(groupName) + "/users/" + username));
    }

    @Then("^the group \"([^\"]*)\" should have the following users$")
    public void the_group_should_have_the_following_users(String groupName, List<String> expectedUsers) throws Throwable {
        I_get_the_group(groupName);
        RestResponse<Group> response = JsonUtil.read(Context.getInstance().getRestResponse(), Group.class);
        Group group = response.getData();
        assertNotNull(group);
        assertNotNull(group.getUsers());
        assertEquals(Sets.newHashSet(expectedUsers), group.getUsers());
    }

    @Given("^I have added to the group \"([^\"]*)\" users$")
    public void I_have_added_to_the_group_users(String groupName, List<String> userNames) throws Throwable {
        for (String username : userNames) {
            I_add_the_user_to_the_group(username, groupName);
            commonSteps.I_should_receive_a_RestResponse_with_no_error();
        }
    }

    @When("^I remove the user \"([^\"]*)\" from the group \"([^\"]*)\"$")
    public void I_remove_the_user_from_the_group(String username, String groupName) throws Throwable {
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().delete("/rest/v1/groups/" + Context.getInstance().getGroupId(groupName) + "/users/" + username));
    }

    @Then("^the user \"([^\"]*)\" should have the following group roles$")
    public void the_user_should_have_the_following_group_roles(String username, List<String> expectedGroupRoles) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        User user = JsonUtil.read(response, User.class).getData();
        assertNotNull(user);
        assertNotNull(user.getGroupRoles());
        assertEquals(Sets.newHashSet(expectedGroupRoles), user.getGroupRoles());

    }

    @Then("^the user \"([^\"]*)\" should have the following group$")
    public void the_user_should_have_the_following_group(String username, List<String> expectedGroups) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        User user = JsonUtil.read(response, User.class).getData();
        assertNotNull(user);
        assertNotNull(user.getGroups());

        Set<String> expectedGroupsSet = Sets.newHashSet();
        for (String expectedGroup : expectedGroups) {
            expectedGroupsSet.add(Context.getInstance().getGroupId(expectedGroup));
        }

        assertEquals(expectedGroupsSet, user.getGroups());

    }

    @Then("^the group \"([^\"]*)\" should not have any users$")
    public void the_group_should_not_have_any_users(String groupName) throws Throwable {
        I_get_the_group(groupName);
        RestResponse<Group> response = JsonUtil.read(Context.getInstance().getRestResponse(), Group.class);
        Group group = response.getData();
        assertNotNull(group);
        assertTrue(CollectionUtils.isEmpty(group.getUsers()));
    }

    @Then("^the user \"([^\"]*)\" should not have any group roles$")
    public void the_user_should_not_have_any_group_roles(String username) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        User user = JsonUtil.read(response, User.class).getData();
        assertNotNull(user);
        assertTrue(CollectionUtils.isEmpty(user.getGroupRoles()));
    }

    @Then("^the user \"([^\"]*)\" should not have any group$")
    public void the_user_should_not_have_any_group(String username) throws Throwable {
        String response = Context.getRestClientInstance().get("/rest/v1/users/" + username);
        User user = JsonUtil.read(response, User.class).getData();
        assertNotNull(user);
        assertTrue(CollectionUtils.isEmpty(user.getGroups()));
    }

    @When("^I get the groups$")
    public void I_get_the_groups(List<String> names) throws Throwable {
        List<String> ids = Lists.newArrayList();
        for (String name : names) {
            ids.add(Context.getInstance().getGroupId(name));
        }
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/groups/getGroups", JsonUtil.toString(ids)));
    }

    @Then("^The RestResponse should contain the groups named$")
    public void The_RestResponse_should_contain_the_groups_named(List<String> expectedNames) throws Throwable {
        RestResponse<?> response = JsonUtil.read(Context.getInstance().getRestResponse());
        assertNotNull(response.getData());
        List<Group> groups = JsonUtil.toList(JsonUtil.toString(response.getData()), Group.class);

        String[] expectedNamesArray = expectedNames.toArray(new String[expectedNames.size()]);
        String[] actualNames = null;
        for (Group group : groups) {
            actualNames = ArrayUtils.add(actualNames, group.getName());
        }

        Arrays.sort(expectedNamesArray);
        Arrays.sort(actualNames);

        assertArrayEquals(expectedNamesArray, actualNames);
    }

    @When("^I update the \"([^\"]*)\" group's name to \"([^\"]*)\"$")
    public void I_update_the_group_name_to(String name, String newName) throws Throwable {
        I_update_the_group_fields(name, Lists.newArrayList(new Entry("name", newName)));
        Context.getInstance().registerGroupId(newName, Context.getInstance().getGroupId(name));
    }

    @When("^I update the \"([^\"]*)\" group fields:$")
    public void I_update_the_group_fields(String name, List<Entry> fields) throws Throwable {
        Map<String, String> fieldsMap = Maps.newHashMap();
        for (Entry field : fields) {
            fieldsMap.put(field.getName(), field.getValue());
        }
        Context.getInstance().registerRestResponse(
                Context.getRestClientInstance().putJSon("/rest/v1/groups/" + Context.getInstance().getGroupId(name), JsonUtil.toString(fieldsMap)));
    }

    @Then("^There should be a group \"([^\"]*)\" in the system$")
    public void There_should_be_a_group_in_the_system(String name) throws Throwable {
        I_get_the_group(name);
        commonSteps.I_should_receive_a_RestResponse_with_no_error();
        Group group = JsonUtil.read(Context.getInstance().getRestResponse(), Group.class).getData();
        assertNotNull(group);
        assertEquals(name, group.getName());
    }

    @When("^I create a new group in the system with name \"([^\"]*)\" , a role \"([^\"]*)\" and a user \"([^\"]*)\"$")
    public void I_create_a_new_group_in_the_system_with_name_a_role_and_a_user(String name, String role, String username) throws Throwable {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setName(name);
        request.setRoles(Sets.newHashSet(role));
        request.setUsers(Sets.newHashSet(username));
        Context.getInstance().registerRestResponse(Context.getRestClientInstance().postJSon("/rest/v1/groups/", JsonUtil.toString(request)));
        String groupId = JsonUtil.read(Context.getInstance().getRestResponse(), String.class).getData();
        if (groupId != null) {
            Context.getInstance().registerGroupId(name, groupId);
        }
    }
}
