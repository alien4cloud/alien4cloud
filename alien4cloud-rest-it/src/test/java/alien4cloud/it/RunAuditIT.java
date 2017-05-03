package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/audit"
        //
}, format = { "pretty", "html:target/cucumber/audit", "json:target/cucumber/cucumber-audit.json" })
// @Ignore
public class RunAuditIT {
}
