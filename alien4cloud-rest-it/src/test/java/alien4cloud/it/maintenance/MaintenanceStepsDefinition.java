package alien4cloud.it.maintenance;

import static alien4cloud.it.Context.getRestClientInstance;

import org.alien4cloud.server.MaintenanceUpdateDTO;

import alien4cloud.it.Context;
import cucumber.api.java.en.When;

/**
 * Steps for maintenance mode cucumber rest it.
 */
public class MaintenanceStepsDefinition {

    @When("^I enable maintenance mode$")
    public void enable() throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().post("/rest/v1/maintenance"));
    }

    @When("^I disable maintenance mode$")
    public void disable() throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().delete("/rest/v1/maintenance"));
    }

    @When("^I get maintenance state$")
    public void getState() throws Throwable {
        Context.getInstance().registerRestResponse(getRestClientInstance().get("/rest/v1/maintenance"));
    }

    @When("^I update maintenance state, message: \"([^\"]*)\" percent: (\\d+)$")
    public void updateState(String message, Integer percent) throws Throwable {
        MaintenanceUpdateDTO updateDTO = new MaintenanceUpdateDTO(message, percent);
        Context.getInstance()
                .registerRestResponse(getRestClientInstance().putJSon("/rest/v1/maintenance", Context.getJsonMapper().writeValueAsString(updateDTO)));
    }
}