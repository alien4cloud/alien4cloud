package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
"classpath:alien/rest/orchestrator-location"
//
}, format = { "pretty",
        "html:target/cucumber/orchestrators-locations",
        "json:target/cucumber/cucumber-orchestrators-locations.json" })
public class RunOrchestratorsLocationsIT {
}

