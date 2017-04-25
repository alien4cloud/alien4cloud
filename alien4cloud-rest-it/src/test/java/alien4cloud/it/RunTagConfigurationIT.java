package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/tags"
        // "classpath:alien/rest/tags/meta_prop_configuration.feature"
}, format = { "pretty", "html:target/cucumber/tags", "json:target/cucumber/cucumber-tags.json" })
// @Ignore
public class RunTagConfigurationIT {
}
