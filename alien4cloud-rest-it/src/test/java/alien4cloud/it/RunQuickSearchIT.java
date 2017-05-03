package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/quicksearch"
        //
}, format = { "pretty", "html:target/cucumber/quicksearch", "json:target/cucumber/cucumber-quicksearch.json" })
// @Ignore
public class RunQuickSearchIT {
}
