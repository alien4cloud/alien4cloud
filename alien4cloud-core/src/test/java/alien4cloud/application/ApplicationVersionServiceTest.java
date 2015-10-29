package alien4cloud.application;

import java.util.Date;
import java.util.UUID;

import javax.annotation.Resource;

import alien4cloud.model.deployment.Deployment;
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
        String versionId = UUID.randomUUID().toString();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId(UUID.randomUUID().toString());
        deployment.setEndDate(null);
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(versionId));
    }

    @Test
    public void versionShouldNotBeDeployedDeploymentComplete() {
        String versionId = UUID.randomUUID().toString();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId(versionId);
        deployment.setEndDate(new Date());
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(versionId));
    }

    @Test
    public void versionBeDeployed() {
        String versionId = UUID.randomUUID().toString();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId(versionId);
        deployment.setEndDate(null);
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertTrue(appVersionSrv.isApplicationVersionDeployed(versionId));
    }
}
