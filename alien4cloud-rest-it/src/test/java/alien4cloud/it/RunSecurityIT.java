package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/security" }, format = { "pretty", "html:target/cucumber/security",
        "json:target/cucumber/cucumber-security.json" })
public class RunSecurityIT {
}
