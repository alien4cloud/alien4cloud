package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/properties"
        //
}, format = { "pretty", "html:target/cucumber/properties", "json:target/cucumber/cucumber-properties.json" })
// @Ignore
public class RunPropertiesIT {

}
