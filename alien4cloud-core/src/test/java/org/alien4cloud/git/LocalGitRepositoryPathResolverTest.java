package org.alien4cloud.git;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

public class LocalGitRepositoryPathResolverTest {

    private LocalGitRepositoryPathResolver resolver;

    @Before
    public void setUp() throws Exception {
        resolver = new LocalGitRepositoryPathResolver();
        Path gitRootPath = Paths.get("src/test/resources/git_structure");
        resolver.setStorageRootPath(gitRootPath.toString());
    }

    @Test
    public void findAllLocalDeploymentConfigGitPath() throws Exception {
        assertThat(resolver.findAllEnvironmentSetupLocalPath("my_app")).hasSize(2);
    }

}