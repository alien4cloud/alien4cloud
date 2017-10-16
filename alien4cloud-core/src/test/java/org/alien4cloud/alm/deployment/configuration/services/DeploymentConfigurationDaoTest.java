package org.alien4cloud.alm.deployment.configuration.services;

import alien4cloud.dao.IGenericSearchDAO;
import org.alien4cloud.alm.deployment.configuration.model.AbstractDeploymentConfig;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentInputs;
import org.alien4cloud.alm.deployment.configuration.model.DeploymentMatchingConfiguration;
import org.alien4cloud.git.LocalGitRepositoryPathResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DeploymentConfigurationDaoTest {

    private DeploymentConfigurationDao dao;

    @Mock
    private LocalGitRepositoryPathResolver localGitRepositoryPathResolver;
    @Mock
    private IGenericSearchDAO alienDao;

    @Before
    public void setUp() throws Exception {
        // No file of type DeploymentInputs are present in git
        when(localGitRepositoryPathResolver.resolve(eq(DeploymentInputs.class), anyString())).thenReturn(Paths.get("/target/DONT_EXIST"));

        dao = new DeploymentConfigurationDao(localGitRepositoryPathResolver, alienDao);
    }

    @Test
    public void migrate_data_previously_store_in_elastic_search() throws Exception {
        String existingId = AbstractDeploymentConfig.generateId("versionOK", "envId");

        // given a configuration store in ES
        when(alienDao.findById(DeploymentMatchingConfiguration.class, existingId)).thenReturn(new DeploymentMatchingConfiguration("versionOK", "envId"));
        // given the location of the existing config is defined
        Path configLocalPath = Paths.get("target/deployment_config_dao_test/config");
        Files.deleteIfExists(configLocalPath);
        when(localGitRepositoryPathResolver.resolve(eq(DeploymentMatchingConfiguration.class), eq(existingId))).thenReturn(configLocalPath);

        // when looking for a config that not existing in Git yet
        DeploymentMatchingConfiguration config = dao.findById(DeploymentMatchingConfiguration.class, existingId);

        // then we get data from ES
        assertThat(config).isNotNull();
        assertThat(config.getId()).isEqualTo(existingId);
        // ES data has been deleted
        verify(alienDao).delete(DeploymentMatchingConfiguration.class, existingId);
        // check data has been migrated into git
        assertThat(Files.exists(configLocalPath)).isTrue();
    }

    @Test
    public void when_no_data_in_elastic_search_and_git() throws Exception {
        assertThat(dao.findById(DeploymentInputs.class, "unknown")).isNull();
    }
}