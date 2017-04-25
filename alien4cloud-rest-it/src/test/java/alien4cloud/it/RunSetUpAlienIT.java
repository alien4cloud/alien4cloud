package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.Ignore;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/setup/it_setup_alien.feature"
        // "classpath:alien/rest/setup/it_setup_mock.feature",
        // "classpath:alien/rest/setup/it_setup_cloudify2.feature",
        // "classpath:alien/rest/setup/it_setup_cloudify3.feature"
})
@Ignore
public class RunSetUpAlienIT {
}
