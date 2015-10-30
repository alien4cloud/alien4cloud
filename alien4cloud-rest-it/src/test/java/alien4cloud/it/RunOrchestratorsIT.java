package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/orchestrators",
        // "classpath:alien/rest/orchestrators/enable_orchestrator.feature",
        // "classpath:alien/rest/orchestrators/update_orchestrator.feature"
}, format = { "pretty", "html:target/cucumber/orchestrators", "json:target/cucumber/cucumber-orchestrators.json" })
public class RunOrchestratorsIT {
}
