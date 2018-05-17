package alien4cloud.configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class TranslationConfigurationServiceTest {
    @Resource
    private TranslationConfigurationService translationConfigurationService;

    @Test
    public void shouldBeAbleToListSupportedLanguagesFromClassPath() {
        translationConfigurationService.getSupportedLanguages();
        // Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(applicationVersion));
    }
}
