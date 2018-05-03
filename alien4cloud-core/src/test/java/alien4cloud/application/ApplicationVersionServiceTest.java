package alien4cloud.application;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Maps;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.model.application.ApplicationTopologyVersion;
import alien4cloud.model.application.ApplicationVersion;
import alien4cloud.model.deployment.Deployment;

//import alien4cloud.model.deployment.DeploymentSetup;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
@DirtiesContext
public class ApplicationVersionServiceTest {
    @Resource
    private ApplicationVersionService appVersionSrv;
    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO dao;

    private ApplicationVersion createApplicationVersion() {
        String version = "1.0.0-SNAPSHOT";
        ApplicationVersion applicationVersion = new ApplicationVersion();
        applicationVersion.setApplicationId("application");
        applicationVersion.setVersion(version);
        LinkedHashMap<String, ApplicationTopologyVersion> topologyVersionMap = Maps.newLinkedHashMap();
        ApplicationTopologyVersion applicationTopologyVersion = new ApplicationTopologyVersion();
        applicationTopologyVersion.setArchiveId(version);
        topologyVersionMap.put(version, applicationTopologyVersion);
        applicationTopologyVersion = new ApplicationTopologyVersion();
        String devVersion = version + "-DEV";
        applicationTopologyVersion.setArchiveId(devVersion);
        topologyVersionMap.put(devVersion, applicationTopologyVersion);
        applicationVersion.setTopologyVersions(topologyVersionMap);

        dao.save(applicationVersion);
        return applicationVersion;
    }

    @Test
    public void versionShouldNotBeDeployedWhenNoDeployment() {
        dao.delete(Deployment.class, QueryBuilders.matchAllQuery());
        // Check is now performed on application versions
        ApplicationVersion applicationVersion = createApplicationVersion();
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(applicationVersion));
    }

    @Test
    public void versionShouldNotBeDeployedDeploymentOnOtherVersion() {
        dao.delete(Deployment.class, QueryBuilders.matchAllQuery());
        ApplicationVersion applicationVersion = createApplicationVersion();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId(UUID.randomUUID().toString());
        deployment.setEndDate(null);
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(applicationVersion));
    }

    @Test
    public void versionShouldNotBeDeployedDeploymentComplete() {
        dao.delete(Deployment.class, QueryBuilders.matchAllQuery());
        ApplicationVersion applicationVersion = createApplicationVersion();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId("1.0.0-SNAPSHOT");
        deployment.setEndDate(new Date());
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertFalse(appVersionSrv.isApplicationVersionDeployed(applicationVersion));
    }

    @Test
    public void versionBeDeployed() {
        dao.delete(Deployment.class, QueryBuilders.matchAllQuery());
        ApplicationVersion applicationVersion = createApplicationVersion();
        Deployment deployment = new Deployment();
        deployment.setId(UUID.randomUUID().toString());
        deployment.setVersionId("1.0.0-SNAPSHOT");
        deployment.setEndDate(null);
        dao.save(deployment);
        // this is supposed to find if a matching deployment object exists in ES.
        Assert.assertTrue(appVersionSrv.isApplicationVersionDeployed(applicationVersion));
    }
}
