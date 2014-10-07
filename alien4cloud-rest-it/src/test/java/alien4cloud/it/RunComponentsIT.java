package alien4cloud.it;

import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = { "classpath:alien/rest/components" }, format = { "pretty", "html:target/cucumber/components",
        "json:target/cucumber/cucumber-components.json" })
public class RunComponentsIT {
}
