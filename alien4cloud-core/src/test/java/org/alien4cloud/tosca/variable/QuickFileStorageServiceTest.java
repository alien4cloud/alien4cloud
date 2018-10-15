package org.alien4cloud.tosca.variable;

import java.nio.file.Path;

import javax.inject.Inject;

import org.alien4cloud.tosca.variable.service.QuickFileStorageService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.application.ApplicationService;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext
public class QuickFileStorageServiceTest {
    @Inject
    private QuickFileStorageService quickFileStorageService;

    @Inject
    private ApplicationService applicationService;

    @Test
    public void afterApplicationDeletedEventListener() throws Exception {
        String applicationId = applicationService.create("admin", "TEST", "TEST", "", null);
        quickFileStorageService.loadApplicationVariables(applicationId);
        Path filePath = quickFileStorageService.getApplicationVariablesPath(applicationId);
        Assertions.assertThat(filePath).exists();

        applicationService.delete(applicationId);
        Assertions.assertThat(filePath).doesNotExist();
    }

}