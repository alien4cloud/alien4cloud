package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

/**
 * Run integration tests for maintenance mode management.
 */
@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/maintenance"
        //
}, format = { "pretty", "html:target/cucumber/groups", "json:target/cucumber/cucumber-groups.json" })

public class RunMaintenanceModeIT {
}
