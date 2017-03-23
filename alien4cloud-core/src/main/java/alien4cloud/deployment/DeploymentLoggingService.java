package alien4cloud.deployment;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.model.PaaSDeploymentLog;
import groovy.util.logging.Log4j;

/**
 * Service is used by some premium plugins for archive the deployments logs.
 */
@Log4j
@Service
public class DeploymentLoggingService {
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Value("${logs_deployment_appender.enable}")
    private boolean isEnable;

    public synchronized void save(final PaaSDeploymentLog deploymentLog) {
        try {
            if (isEnable) {
                Logger currentLogger = LogManager.getLogger("DEPLOYMENT_LOGS_LOGGER");
                currentLogger.info(deploymentLog.toCompactString());
            }
        } finally {
            alienMonitorDao.save(deploymentLog);
        }
    }

    public void save(final PaaSDeploymentLog[] deploymentLogs) {
        for (PaaSDeploymentLog log : deploymentLogs) {
            save(log);
        }
    }

}