package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/users" }, format = { "pretty", "html:target/cucumber/users", "json:target/cucumber/cucumber-users.json" })
// @Ignore
public class RunUsersIT {
}
