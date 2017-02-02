package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
         "classpath:alien/rest/service"
        //
}, format = { "pretty", "html:target/cucumber/service", "json:target/cucumber/cucumber-service.json" })
// @Ignore
public class RunServiceIT {
}
