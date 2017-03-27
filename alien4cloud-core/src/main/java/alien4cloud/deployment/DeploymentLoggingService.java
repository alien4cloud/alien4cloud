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
    private boolean isEnabled;

    private final Logger deployments_logger = LogManager.getLogger("DEPLOYMENT_LOGS_LOGGER");

    public synchronized void save(final PaaSDeploymentLog deploymentLog) {
        try {
            if (isEnabled) {
                deployments_logger.info(deploymentLog.toCompactString());
            }
        } finally {
            alienMonitorDao.save(deploymentLog);
        }
    }

    public synchronized void save(final PaaSDeploymentLog[] deploymentLogs) {
        try {
            if (isEnabled) {
                for (PaaSDeploymentLog log : deploymentLogs) {
                    deployments_logger.info(log.toCompactString());
                }
            }
        } finally {
            alienMonitorDao.save(deploymentLogs);
        }
    }

}