package alien4cloud.it;

import org.junit.Ignore;
import org.junit.runner.RunWith;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/components"
        // "classpath:alien/rest/components/search_components_enhanced.feature"
//         "classpath:alien/rest/components/search_components.feature"
}, format = { "pretty", "html:target/cucumber/components", "json:target/cucumber/cucumber-components.json" })
// @Ignore
public class RunComponentsIT {
}
