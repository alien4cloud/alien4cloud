package alien4cloud.deployment;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.alien4cloud.tosca.model.CSARDependency;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.deployment.DeploymentTopology;

/**
 * Unit tests for deployment service.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DeploymentServiceTest {

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Inject
    private DeploymentService deploymentService;

    @Test
    public void isArchiveDeployedTest() {
        DeploymentTopology deploymentTopology = new DeploymentTopology();
        deploymentTopology.setId("id");
        deploymentTopology.setDeployed(false);
        deploymentTopology.setDependencies(Sets.newHashSet(new CSARDependency("toto", "2.0.0")));
        alienMonitorDao.save(deploymentTopology);

        deploymentTopology.setId("id2");
        deploymentTopology.setDeployed(true);
        deploymentTopology.setDependencies(Sets.newHashSet(new CSARDependency("tata", "1.0.0")));
        alienMonitorDao.save(deploymentTopology);

        deploymentTopology.setId("id3");
        deploymentTopology.setDeployed(true);
        deploymentTopology.setDependencies(Sets.newHashSet(new CSARDependency("toto", "1.0.0")));
        alienMonitorDao.save(deploymentTopology);

        Assert.assertFalse(deploymentService.isArchiveDeployed("toto", "2.0.0"));

        deploymentTopology.setId("id");
        deploymentTopology.setDeployed(true);
        deploymentTopology.setDependencies(Sets.newHashSet(new CSARDependency("toto", "2.0.0")));
        alienMonitorDao.save(deploymentTopology);

        Assert.assertTrue(deploymentService.isArchiveDeployed("toto", "2.0.0"));
    }
}
