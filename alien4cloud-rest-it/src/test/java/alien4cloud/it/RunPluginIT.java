package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/plugin"
        //
}, format = { "pretty", "html:target/cucumber/plugin", "json:target/cucumber/cucumber-plugin.json" })
// @Ignore
public class RunPluginIT {
}
