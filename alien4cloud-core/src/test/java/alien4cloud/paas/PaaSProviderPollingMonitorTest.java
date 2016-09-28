package alien4cloud.paas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;

import javax.annotation.Resource;

import org.elasticsearch.client.Client;
import org.elasticsearch.mapping.ElasticSearchClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.PaaSProviderPollingMonitor;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSMessageMonitorEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test monitoring events recovery
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context-test.xml")
public class PaaSProviderPollingMonitorTest {

    @Resource(name = "alien-es-dao")
    private IGenericSearchDAO alienDao;

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Resource
    ElasticSearchClient esclient;
    Client nodeClient;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private Date latestEventDate = null;
    private Date latestStatusDate = null;

    @Before
    public void initMocks() throws IOException, InterruptedException {
        nodeClient = esclient.getClient();
    }

    private void initEvents() throws JsonProcessingException, InterruptedException {

        // add 2 message events
        PaaSMessageMonitorEvent eventMessage = null;
        String eventJson = null;
        int i = 0;

        for (i = 0; i < 2; i++) {

            eventMessage = new PaaSMessageMonitorEvent();
            eventMessage.setOrchestratorId("CloudID");
            eventMessage.setDate(addMinutesToDate(2, new Date()).getTime());
            eventMessage.setDeploymentId("ID-XXX+" + i);
            eventMessage.setMessage("EVENT MESSAGE : " + eventMessage.getDate());
            eventJson = jsonMapper.writeValueAsString(eventMessage);

            nodeClient.prepareIndex("deploymentmonitorevents", PaaSMessageMonitorEvent.class.getSimpleName().toLowerCase()).setSource(eventJson)
                    .setRefresh(true).execute().actionGet();
        }

        // add 3 deployment status events
        PaaSDeploymentStatusMonitorEvent eventDeploymentStatus = null;

        for (i = 0; i < 3; i++) {

            eventDeploymentStatus = new PaaSDeploymentStatusMonitorEvent();
            eventDeploymentStatus.setOrchestratorId("CloudID");
            eventDeploymentStatus.setDate(addMinutesToDate(2, new Date()).getTime());
            eventDeploymentStatus.setDeploymentId("DEP_ID-" + i);
            eventDeploymentStatus.setDeploymentStatus(DeploymentStatus.DEPLOYED);
            eventJson = jsonMapper.writeValueAsString(eventDeploymentStatus);

            nodeClient.prepareIndex("deploymentmonitorevents", PaaSDeploymentStatusMonitorEvent.class.getSimpleName().toLowerCase()).setSource(eventJson)
                    .setRefresh(true).execute().actionGet();
        }

        // save the last inserted date (PaaSDeploymentStatusMonitorEvent should be generated from alien only and never from the orchestrator).
        latestEventDate = new Date(eventMessage.getDate());
        latestStatusDate = new Date(eventDeploymentStatus.getDate());
    }

    @Test
    public void testLoadEventsFromLastRegistered()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, JsonProcessingException, InterruptedException {

        // init with some events
        initEvents();

        PaaSProviderPollingMonitor paaSProviderPollingMonitor = new PaaSProviderPollingMonitor(alienDao, alienMonitorDao, null, null, "CloudID");
        Field lastPollingDateField = PaaSProviderPollingMonitor.class.getDeclaredField("lastPollingDate");
        lastPollingDateField.setAccessible(true);
        Date lastDate = (Date) lastPollingDateField.get(paaSProviderPollingMonitor);

        assertNotEquals(latestStatusDate, lastDate);
        assertEquals(latestEventDate, lastDate);
    }

    @Test
    public void testLoadEventsWithoutEvents() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        PaaSProviderPollingMonitor paaSProviderPollingMonitor = new PaaSProviderPollingMonitor(alienDao, alienMonitorDao, null, null, "CloudID");
        Field lastPollingDateField = PaaSProviderPollingMonitor.class.getDeclaredField("lastPollingDate");
        lastPollingDateField.setAccessible(true);
        Date lastDate = (Date) lastPollingDateField.get(paaSProviderPollingMonitor);

        // lastDate should be a new Date() initialized in PaaSProviderPollingMonitor constructor
        assertTrue(lastDate.after(new Date()));
    }

    private Date addMinutesToDate(int minutes, Date beforeTime) {
        final long ONE_MINUTE_IN_MILLIS = 60000;// millisecs
        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs + (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }

}
