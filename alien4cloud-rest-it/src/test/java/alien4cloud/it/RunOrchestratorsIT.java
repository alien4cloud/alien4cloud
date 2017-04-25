package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/orchestrators"
        // "classpath:alien/rest/orchestrators/enable_orchestrator.feature",
        // "classpath:alien/rest/orchestrators/update_orchestrator.feature"
        // "classpath:alien/rest/orchestrators/orchestrator-crud.feature",
}, format = { "pretty", "html:target/cucumber/orchestrators", "json:target/cucumber/cucumber-orchestrators.json" })
// @Ignore
public class RunOrchestratorsIT {
}
