package alien4cloud.application;

import java.util.UUID;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.IGenericSearchDAO;

//import alien4cloud.model.deployment.DeploymentSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class ApplicationVersionServiceTest {
    @Resource
    private ApplicationVersionService appVersionSrv;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    @Test
    public void versionShouldNotBeDeployedWhenNoDeployment() {
        String versionId = UUID.randomUUID().toString();
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(versionId));
    }

    @Test
    public void versionShouldNotBeDeployedDeploymentOnOtherVersion() {
        // String versionId = UUID.randomUUID().toString();
        // DeploymentSetup deploymentSetup = new DeploymentSetup();
        // deploymentSetup.setId(UUID.randomUUID().toString());
        // deploymentSetup.setVersionId(UUID.randomUUID().toString());
        // Deployment deployment = new Deployment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), DeploymentSourceType.APPLICATION,
        // UUID.randomUUID().toString(), new String[] { UUID.randomUUID().toString() }, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        // UUID.randomUUID().toString(), new Date(), null, deploymentSetup);
        // dao.save(deployment);
        // // this is supposed to find if a matching deployment object exists in ES.
        // Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(versionId));
        Assert.fail("Failed");
    }

    @Test
    public void versionShouldNotBeDeployedDeploymentComplete() {
        // String versionId = UUID.randomUUID().toString();
        // DeploymentSetup deploymentSetup = new DeploymentSetup();
        // deploymentSetup.setId(UUID.randomUUID().toString());
        // deploymentSetup.setVersionId(versionId);
        // Deployment deployment = new Deployment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), DeploymentSourceType.APPLICATION,
        // UUID.randomUUID().toString(), new String[] { UUID.randomUUID().toString() }, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        // UUID.randomUUID().toString(), new Date(), new Date(), deploymentSetup);
        // dao.save(deployment);
        // // this is supposed to find if a matching deployment object exists in ES.
        // Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(versionId));
        Assert.fail("Fix test");
    }

    @Test
    public void versionBeDeployed() {
        // String versionId = UUID.randomUUID().toString();
        // DeploymentSetup deploymentSetup = new DeploymentSetup();
        // deploymentSetup.setId(UUID.randomUUID().toString());
        // deploymentSetup.setVersionId(versionId);
        // Deployment deployment = new Deployment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), DeploymentSourceType.APPLICATION,
        // UUID.randomUUID().toString(), new String[] { UUID.randomUUID().toString() }, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        // UUID.randomUUID().toString(), new Date(), null, deploymentSetup);
        // dao.save(deployment);
        // // this is supposed to find if a matching deployment object exists in ES.
        // Assert.assertTrue(appVersionSrv.isApplicationVersionDeployed(versionId));
        Assert.fail("Fix test");
    }
}
