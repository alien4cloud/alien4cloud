package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/groups"
        //
}, format = { "pretty", "html:target/cucumber/groups", "json:target/cucumber/cucumber-groups.json" })
// @Ignore
public class RunGroupsIT {
}
