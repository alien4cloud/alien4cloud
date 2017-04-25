package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/orchestrator-location"
        // "classpath:alien/rest/orchestrator-location/location_resources_authorizations.feature"
        //
}, format = { "pretty", "html:target/cucumber/orchestrators-locations", "json:target/cucumber/cucumber-orchestrators-locations.json" })
// @Ignore
public class RunOrchestratorsLocationsIT {
}
