package alien4cloud.it;

import cucumber.api.CucumberOptions;
import cucumber.api.java.en.Then;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

import static org.alien4cloud.test.util.SPELUtils.evaluateAndAssertExpression;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        //
        "classpath:alien/rest/service",
        // "classpath:alien/rest/service/create_service.feature",
//         "classpath:alien/rest/service/delete_service.feature",
//         "classpath:alien/rest/service/delete_used_service.feature",
        // "classpath:alien/rest/service/get_service.feature",
        // "classpath:alien/rest/service/list_service.feature",
        // "classpath:alien/rest/service/search_service.feature",
        // "classpath:alien/rest/service/update_service.feature",
        // "classpath:alien/rest/service/patch_service.feature",
        //
}, format = { "pretty", "html:target/cucumber/service", "json:target/cucumber/cucumber-service.json" })
// @Ignore
public class RunServiceIT {
}
